package ch.turic.embed;

import ch.turic.BuiltIns;
import ch.turic.exceptions.BadSyntax;
import ch.turic.exceptions.ExecutionException;
import ch.turic.exceptions.InterpreterHalt;
import ch.turic.memory.GlobalContext;
import ch.turic.memory.LocalContext;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;

/**
 * One isolated interpreter instance: its own global variables, step counters, and limits, as
 * configured by the owning {@link TuriEngine}'s {@link SandboxPolicy}.
 * <p>
 * A session is <b>not</b> thread-safe; create it, use it, and close it on a single thread.
 * Variables injected with {@link #set(String, Object)} appear to the script as frozen global
 * variables. State (global variables) persists between {@link #eval} calls of the same
 * session, so a session can run a program in several steps.
 * <p>
 * After a wall-clock timeout ({@link TuriTimeoutException}) the session is aborted and cannot
 * be used again; create a new session instead. After a step-limit halt, the session remains
 * usable, but the step counter is <em>not</em> reset automatically: the limit caps the total
 * steps of the session unless the embedder grants a fresh budget with {@link #resetSteps()}.
 */
public final class TuriSession implements AutoCloseable {
    private final TuriEngine engine;
    private final GlobalContext globalContext;
    private final LocalContext ctx;
    private volatile boolean timedOut = false;
    private boolean closed = false;

    TuriSession(TuriEngine engine) {
        this.engine = engine;
        final var policy = engine.policy();
        this.globalContext = new GlobalContext(policy.stepLimit(), policy.graceSteps());
        globalContext.setExecutor(engine.taskExecutor());
        globalContext.setThreadPermits(engine.threadPermits());
        if (policy.out() != null) {
            globalContext.setOut(policy.out());
        }
        if (policy.err() != null) {
            globalContext.setErr(policy.err());
        }
        // Phase 2: capability gating, class filter, and import-root scoping. Must be set
        // before BuiltIns.register runs, so that gated built-ins are filtered out.
        globalContext.setGrantedCapabilities(policy.grantedCapabilities());
        globalContext.setImportRoot(policy.importRoot());
        globalContext.setFileReadRoots(policy.fileReadRoots());
        globalContext.setFileReadWriteRoots(policy.fileReadWriteRoots());
        globalContext.setMaxMappedBytes(policy.maxMappedBytes());
        if (policy.isDenyByDefault()
                && policy.grantedCapabilities().contains(ch.turic.Capability.FILE_TEMP)) {
            // eager: the scratch directory is the implicit read-write root of an untrusted
            // FILE_TEMP grant, so it must exist (and confine) before the first file built-in
            // runs, not only after the first tmp_file() call
            globalContext.tempRoot();
        }
        if (policy.classFilter() != null) {
            globalContext.classLoader.setScriptClassFilter(policy.classFilter(), policy.modeLabel());
        }
        this.ctx = new LocalContext(globalContext);
        BuiltIns.registerGlobalConstants(ctx);
        BuiltIns.register(ctx);
    }

    /**
     * Injects a value as a frozen global variable, visible to every subsequent {@link #eval}
     * call of this session. The script can read but not reassign it.
     *
     * @param name  the global variable name
     * @param value the value; Java values are visible to the script through the usual
     *              interoperability conversions
     * @return this session, for chaining
     */
    public TuriSession set(String name, Object value) {
        ensureUsable();
        ctx.global(Objects.requireNonNull(name), value);
        ctx.freeze(name);
        return this;
    }

    /**
     * Compiles and evaluates the given source code; a convenience shorthand for
     * {@code eval(engine.compile(source))}. Prefer {@link TuriEngine#compile(String)} plus
     * {@link #eval(TuriProgram)} when the same source runs more than once.
     *
     * @param source the Turicum source code
     * @return the result of the last evaluated expression
     * @throws BadSyntax if the source code contains syntax errors
     */
    public Object eval(String source) throws BadSyntax {
        return eval(engine.compile(source));
    }

    /**
     * Evaluates a compiled program in this session, enforcing the engine's policy.
     * <p>
     * When the policy has a {@link SandboxPolicy#timeout() timeout}, a watchdog aborts every
     * thread of this session once it fires, and this method throws a
     * {@link TuriTimeoutException}; the session cannot be used afterwards. Step-limit and
     * abort halts are reported as {@link ExecutionException} with a script-level stack trace,
     * the same way {@link ch.turic.Interpreter} reports them.
     *
     * @param program the compiled program to evaluate
     * @return the result of the last evaluated expression
     * @throws ExecutionException   if the script fails or hits a resource limit
     * @throws TuriTimeoutException if the wall-clock timeout fires
     */
    public Object eval(TuriProgram program) {
        ensureUsable();
        // the session may legitimately be created by a pooling thread and used by a worker;
        // make sure the watchdog interrupts the thread that actually runs the script
        ctx.threadContext.setThread(Thread.currentThread());
        final ScheduledFuture<?> fuse = engine.scheduleTimeout(() -> {
            timedOut = true;
            globalContext.abortAll();
        });
        try {
            return program.program.execute(ctx);
        } catch (ExecutionException | InterpreterHalt e) {
            if (timedOut) {
                throw new TuriTimeoutException(engine.policy().timeout(), e);
            }
            throw adaptStackTrace(e);
        } finally {
            if (fuse != null) {
                fuse.cancel(false);
            }
            globalContext.joinThreads();
            // a late-firing watchdog between the try block and the cancel above must not
            // silently poison the next eval; the aborted flag makes it fail loudly anyway
        }
    }

    /**
     * Reads a global variable of the session, typically to fetch results a script stored
     * beyond the value of its last expression.
     *
     * @param name the global variable name
     * @return the value of the variable; Turicum {@code none} is returned as {@code null}
     * @throws ExecutionException if no such variable is defined
     */
    public Object get(String name) {
        ensureUsable();
        return ctx.get(Objects.requireNonNull(name));
    }

    /**
     * @return the number of interpreter steps this session has executed since it was created
     * or since the last {@link #resetSteps()}; useful for metering and for sizing
     * {@link SandboxPolicy.Builder#stepLimit(int)}
     */
    public long stepsUsed() {
        return globalContext.steps.get();
    }

    /**
     * Resets the session's step counter to zero, so the next evaluation gets the full
     * {@link SandboxPolicy#stepLimit()} budget again. Without a reset, the limit caps the
     * <em>total</em> steps of the session: after a step-limit halt, every further evaluation
     * halts immediately. Call this between evaluations to turn the step limit into a
     * per-evaluation budget — the choice stays with the embedder, never with the script.
     *
     * @return the number of steps consumed since the session was created or the counter was
     * last reset, so metering per evaluation needs no separate bookkeeping
     */
    public long resetSteps() {
        ensureUsable();
        return globalContext.steps.getAndSet(0);
    }

    /**
     * @return {@code true} once the session's timeout fired; the session cannot be used anymore
     */
    public boolean isTimedOut() {
        return timedOut;
    }

    private void ensureUsable() {
        if (closed) {
            throw new IllegalStateException("The TuriSession is closed");
        }
        if (timedOut) {
            throw new IllegalStateException("The TuriSession timed out and cannot be used anymore");
        }
    }

    /**
     * Replaces the Java-level stack trace with the script-level one, the same adaptation that
     * {@link ch.turic.Interpreter#execute(ch.turic.Command)} performs. Halts (step limit,
     * abort) are invisible to Turicum {@code try}/{@code catch}, but at this boundary they are
     * converted to the documented {@link ExecutionException} for the embedder.
     */
    private ExecutionException adaptStackTrace(RuntimeException e) {
        final var newStackTrace = new ArrayList<StackTraceElement>();
        final var stackTrace = ctx.threadContext.getStackTrace();
        for (int i = stackTrace.size() - 1; i >= 0; i--) {
            final var stackFrame = stackTrace.get(i);
            if (stackFrame.command().startPosition() != null) {
                newStackTrace.add(new StackTraceElement(
                        stackFrame.command().getClass().getSimpleName(),
                        "",
                        stackFrame.command().startPosition().file,
                        stackFrame.command().startPosition().line
                ));
            }
        }
        final var turiException = new ExecutionException(e);
        turiException.setStackTrace(newStackTrace.toArray(StackTraceElement[]::new));
        return turiException;
    }

    /**
     * Closes the session: aborts and joins any asynchronous task it still has running,
     * force-closes any file handle the script left open, deletes the session's temp scratch
     * directory, and unregisters its thread context.
     */
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            globalContext.joinThreads();
            globalContext.closeFileResources();
            ctx.close();
        }
    }
}
