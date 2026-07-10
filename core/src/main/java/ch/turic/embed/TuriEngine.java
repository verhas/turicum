package ch.turic.embed;

import ch.turic.Command;
import ch.turic.Input;
import ch.turic.Program;
import ch.turic.analyzer.Lexer;
import ch.turic.analyzer.ProgramAnalyzer;
import ch.turic.exceptions.BadSyntax;
import ch.turic.utils.Unmarshaller;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * The entry point for embedding Turicum into a Java application.
 * <p>
 * An engine pairs a {@link SandboxPolicy} with the resources shared by its sessions: the
 * executor running the scripts' asynchronous tasks, the thread-permit pool implementing the
 * {@link SandboxPolicy#maxThreads()} cap, and the watchdog timer implementing the
 * {@link SandboxPolicy#timeout()} limit. Programs are compiled once and can be evaluated many
 * times in independent {@link TuriSession}s:
 *
 * <!-- the example is Java, not Turicum; the pre tag and the opening code inline tag are kept
 *      on separate lines so that TestJavaDocSnippets does not run it as a Turicum program -->
 * <pre>
 * {@code
 * try (final var engine = TuriEngine.create(policy)) {
 *     final var program = engine.compile("1 + 2");
 *     try (final var session = engine.newSession()) {
 *         session.set("host_data", data);
 *         final var result = session.eval(program);
 *     }
 * }
 * }</pre>
 * <p>
 * An engine is thread-safe; sessions are not — each session must be used by a single thread.
 * Closing the engine stops its executors; sessions should be finished before the engine is
 * closed.
 */
public final class TuriEngine implements AutoCloseable {
    private final SandboxPolicy policy;
    private final ExecutorService taskExecutor;
    private final Semaphore threadPermits;
    private final ScheduledExecutorService watchdog;
    private volatile boolean closed = false;

    private TuriEngine(SandboxPolicy policy) {
        this.policy = policy;
        this.taskExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.threadPermits = policy.maxThreads() < 0 ? null : new Semaphore(policy.maxThreads());
        if (policy.timeout() == null) {
            this.watchdog = null;
        } else {
            this.watchdog = Executors.newSingleThreadScheduledExecutor(r -> {
                final var thread = new Thread(r, "turi-engine-watchdog");
                thread.setDaemon(true);
                return thread;
            });
        }
    }

    /**
     * Creates an engine enforcing the given policy on all its sessions.
     *
     * @param policy the resource limits and redirections for this engine
     * @return the new engine
     */
    public static TuriEngine create(SandboxPolicy policy) {
        return new TuriEngine(Objects.requireNonNull(policy));
    }

    /**
     * Creates an engine without any limits, equivalent to
     * {@code create(SandboxPolicy.UNRESTRICTED)}.
     *
     * @return the new engine
     */
    public static TuriEngine create() {
        return create(SandboxPolicy.UNRESTRICTED);
    }

    /**
     * @return the policy this engine enforces
     */
    public SandboxPolicy policy() {
        return policy;
    }

    /**
     * Compiles Turicum source code into an immutable, reusable program. Compilation runs no
     * script code and needs no limits.
     *
     * @param source the Turicum source code
     * @return the compiled program
     * @throws BadSyntax if the source code contains syntax errors
     */
    public TuriProgram compile(String source) throws BadSyntax {
        ensureOpen();
        final var lexes = Lexer.analyze(Input.fromString(source));
        if (lexes.isEmpty()) {
            return new TuriProgram(new Program(new Command[0]));
        }
        return new TuriProgram((Program) new ProgramAnalyzer().analyze(lexes));
    }

    /**
     * Loads a program from its binary {@code .turc} form, as produced by
     * {@link TuriProgram#serialize()}.
     *
     * @param turc the serialized program
     * @return the loaded program
     */
    public TuriProgram load(byte[] turc) {
        ensureOpen();
        return new TuriProgram(new Unmarshaller().deserialize(turc));
    }

    /**
     * Creates a new session: an isolated interpreter instance with its own global variables,
     * limited and redirected according to this engine's policy. Sessions of one engine share
     * nothing but the engine's executor and thread-permit pool.
     * <p>
     * The session must be used from a single thread and closed when no longer needed.
     *
     * @return the new session
     */
    public TuriSession newSession() {
        ensureOpen();
        return new TuriSession(this);
    }

    ExecutorService taskExecutor() {
        return taskExecutor;
    }

    Semaphore threadPermits() {
        return threadPermits;
    }

    /**
     * Schedules the policy's timeout for one evaluation, or returns {@code null} when the
     * policy has no timeout. The returned future must be cancelled when the evaluation
     * finishes in time.
     */
    ScheduledFuture<?> scheduleTimeout(Runnable abortAction) {
        if (watchdog == null) {
            return null;
        }
        return watchdog.schedule(abortAction, policy.timeout().toNanos(), TimeUnit.NANOSECONDS);
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("The TuriEngine is closed");
        }
    }

    /**
     * Closes the engine: stops the watchdog and the task executor. Asynchronous script tasks
     * that are still running are interrupted. Sessions of this engine should be finished
     * before the engine is closed.
     */
    @Override
    public void close() {
        closed = true;
        if (watchdog != null) {
            watchdog.shutdownNow();
        }
        taskExecutor.shutdownNow();
    }
}
