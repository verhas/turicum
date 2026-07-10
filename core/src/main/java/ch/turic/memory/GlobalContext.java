package ch.turic.memory;

import ch.turic.TuriClass;
import ch.turic.exceptions.ExecutionException;
import ch.turic.exceptions.StepLimitReached;
import ch.turic.memory.debugger.DebuggerContext;
import ch.turic.utils.TuricumClassLoader;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A special context holding a constant string, like built-ins, one for the interpreter.
 * <p>
 * This global context is shared for the whole interpreter for all the threads.
 */
public class GlobalContext {
    // globals can be shared between threads without a synchronization point, so the heap
    // stores the values in volatile variables; see VolatileVariable
    public VarTable heap = new VarTable(true);
    public final int stepLimit;
    /** Extra steps granted to a finally/exit block after a halt, per thread; 0 disables it. See {@link ThreadContext}. */
    public final int graceSteps;
    public final AtomicInteger steps = new AtomicInteger();
    private final Map<Class<?>, TuriClass> turiClasses = new HashMap<>();
    Path sourcePath;
    private boolean debugMode = false; // true when the interpreter is in debug mode
    final DebuggerContext debuggerContext = new DebuggerContext(null, null);
    private final Map<ThreadContext, AtomicInteger> contexts = new ConcurrentHashMap<>();
    public final TuricumClassLoader classLoader = new TuricumClassLoader(getClass().getClassLoader());
    // stores all predefined global symbols, so they do not get exported using export_all()
    public final Set<String> predefinedGlobals = new HashSet<>();

    /**
     * The executor shared by all interpreters that were not configured with their own executor.
     * It is never shut down; virtual threads are cheap and dangling tasks are stopped via
     * {@link ThreadContext#abort()} and {@link #joinThreads()}.
     */
    private static final ExecutorService SHARED_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    // volatile: configured by the embedder on the main thread before execution, read by any
    // interpreter thread that starts an async task or a flow cell
    private volatile ExecutorService executor = SHARED_EXECUTOR;
    private volatile Semaphore threadPermits = null;
    private volatile PrintStream out = System.out;
    private volatile PrintStream err = System.err;

    public GlobalContext(int stepLimit) {
        this(stepLimit, 0);
    }

    /**
     * @param stepLimit  the maximum permitted steps, or a negative value for no limit
     * @param graceSteps extra steps granted to a finally/exit block after a halt fires on a
     *                   thread of this interpreter, or 0 to disable cleanup grace entirely
     */
    public GlobalContext(int stepLimit, int graceSteps) {
        this.stepLimit = stepLimit;
        this.graceSteps = graceSteps;
    }

    public DebuggerContext getDebuggerContext() {
        return debuggerContext;
    }

    /**
     * Retrieves the {@code TuriClass} object associated with the given class. If the exact class is not found
     * in the internal mapping, it attempts to find the superclass or implemented interface
     * that matches the given class.
     * <p>
     * If there are multiple interfaces or classes matching, it finds one randomly.
     *
     * @param clazz the class for which the corresponding {@code TuriClass} object is to be retrieved
     * @return the {@code TuriClass} associated with the given class, or {@code null} if no match is found
     */
    public TuriClass getTuriClass(Class<?> clazz) {
        if (turiClasses.containsKey(clazz)) {
            return turiClasses.get(clazz);
        }
        for (final var turiclass : turiClasses.keySet()) {
            if (turiclass.isAssignableFrom(clazz)) {
                return turiClasses.get(turiclass);
            }
        }
        return null;
    }

    /**
     * Adds a mapping between a Java {@code Class} and a corresponding {@code TuriClass} instance.
     * If the class is already registered as a Turi class, an {@code ExecutionException} is thrown.
     *
     * @param clazz     the Java {@code Class} to be associated with the given {@code TuriClass}
     * @param turiClass the {@code TuriClass} instance to be associated with the Java {@code Class}
     * @throws ExecutionException if the provided class is already registered as a Turi class
     */
    public void addTuriClass(Class<?> clazz, TuriClass turiClass) throws ExecutionException {
        if (turiClasses.containsKey(clazz)) {
            ExecutionException.when(turiClasses.get(clazz).getClass() != turiClass.getClass(), "Class " + clazz.getName() + " already exists as a turi class, cannot be redefined.");
        } else {
            turiClasses.put(clazz, turiClass);
        }
    }

    /**
     * Retrieves the current state of the debug mode.
     *
     * @return {@code true} if the debug mode is enabled, {@code false} otherwise
     */
    public boolean debugMode() {
        return debugMode;
    }

    /**
     * Updates the debug mode state of the interpreter and retrieves the previous state.
     *
     * @param debugMode the new state of the debug mode to set
     * @return the current state of the debug mode before the update
     */
    public boolean debugMode(boolean debugMode) {
        try {
            return this.debugMode;
        } finally {
            this.debugMode = debugMode;
        }
    }

    /**
     * Executes a single computational step based on the defined step limit.
     * This method ensures that the number of executed steps does not exceed the configured step limit.
     * <p>
     * - If the step limit is negative, the method returns immediately without performing any action.
     * - If the current number of steps has reached or exceeded the step limit, a {@code StepLimitReached} is thrown.
     * - Otherwise, the step counter is incremented by one.
     *
     * @throws StepLimitReached if the step limit is reached or exceeded; it is not an
     *                          {@link ExecutionException}, so Turicum-level {@code try}/{@code catch}
     *                          cannot swallow it
     */
    public void step() throws ExecutionException {
        final var currentStep = steps.incrementAndGet();
        if (stepLimit < 0) {
            return;
        }
        if (stepLimit <= currentStep) {
            throw new StepLimitReached(stepLimit);
        }
    }

    /**
     * Registers the given context into the global context registry.
     * Ensures thread-safe addition of the context to the internal collection.
     * <p>
     * One thread context can be attached to many local contexts; therefore, the registry
     * counts the number of local contexts that are attached to the context.
     *
     * @param context the context to be registered
     */
    public void registerContext(ThreadContext context) {
        synchronized (contexts) {
            contexts.computeIfAbsent(context, k -> new AtomicInteger(0)).incrementAndGet();
        }
    }

    /**
     * Removes the specified context from the global context registry.
     * Ensures thread-safe removal of the context from the internal collection.
     * <p>
     * The method decrements the reference counter and if it is zero then it fully removes the thread context from the
     * registry.
     *
     * @param context the context to be removed
     */
    public void removeContext(ThreadContext context) {
        synchronized (contexts) {
            if (!contexts.containsKey(context)) {
                throw new RuntimeException("Context of a thread was not found in registry");
            }
            final var count = contexts.get(context).decrementAndGet();
            if (count == 0) {
                contexts.remove(context);
            }
        }
    }

    /**
     * Joins all threads that are currently registered in the global context, ensuring
     * that the calling thread waits for the other threads to terminate before proceeding.
     * <p>
     * The method iterates over all the contexts in the internal collection and, for each entry,
     * checks if its associated thread is not the current thread. If the thread is different,
     * it joins the thread, blocking execution until the respective thread completes.
     * <p>
     * If the thread is interrupted during the join operation, the exception is caught
     * and silently ignored.
     */
    public void joinThreads() {
        contexts.forEach((k, v) -> {
            try {
                if (k.getDebuggerContext() != null) {
                    k.getDebuggerContext().close();
                }
                if (k.getThread() != null && k.getThread() != Thread.currentThread()) {
                    k.abort();
                    k.getThread().join();
                }

            } catch (InterruptedException ignored) {
            }
        });
    }

    /**
     * Switch the global heap to multithreaded mode, converting the heap from a normal hash map to a concurrent hash
     * map.
     * <p>
     * This method does not need synchronization. When it is called first time, there are no multiple threads.
     * The single main thread that is about to start other threads will wait till the heap is replaced.
     * <p>
     * On subsequent calls, when there are already multiple threads, the replacement does ot happen. It is already done.
     * <p>
     * The boolean flag needs to be atomic, so a second thread does not think she is the only one running and start
     * the replacement of the heap map again.
     * <p>
     * The heap replacement to {@link ConcurrentHashMap} does not guarantee that the variables are also updated.
     */
    public void switchToMultithreading() {
        heap.parallel();
    }

    /**
     * Returns the executor used to run asynchronous tasks (async blocks, flow cells) of this
     * interpreter. By default, it is a JVM-wide shared virtual-thread executor; an embedder can
     * replace it via {@link #setExecutor(ExecutorService)} to get per-engine isolation and
     * deterministic shutdown.
     *
     * @return the executor for this interpreter's asynchronous tasks
     */
    public ExecutorService executor() {
        return executor;
    }

    /**
     * Replaces the executor used for this interpreter's asynchronous tasks.
     * <p>
     * The caller retains ownership of the executor's lifecycle; this class never shuts it down.
     * Must be called before execution starts.
     *
     * @param executor the executor to run async tasks and flow cells on
     */
    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    /**
     * Limits the number of concurrently running interpreter threads by installing a permit pool.
     * <p>
     * The permits may be shared between several interpreters (for an engine-wide cap) by passing
     * the same {@link Semaphore} instance to each of them. Passing {@code null} removes the limit.
     * Must be called before execution starts.
     *
     * @param threadPermits the shared permit pool, or {@code null} for no limit
     */
    public void setThreadPermits(Semaphore threadPermits) {
        this.threadPermits = threadPermits;
    }

    /**
     * Acquires a permit to start a new interpreter thread, if a permit pool is installed.
     * <p>
     * The acquisition never blocks: when the pool is exhausted, the method fails immediately so
     * that a script cannot stall the interpreter by over-spawning.
     *
     * @throws ExecutionException if the thread limit is reached
     */
    public void acquireThreadPermit() throws ExecutionException {
        final var permits = threadPermits;
        if (permits != null && !permits.tryAcquire()) {
            throw new ExecutionException("Thread limit reached, cannot start a new thread");
        }
    }

    /**
     * Returns a permit acquired by {@link #acquireThreadPermit()} to the pool.
     * Called from the finishing thread's {@code finally} block.
     */
    public void releaseThreadPermit() {
        final var permits = threadPermits;
        if (permits != null) {
            permits.release();
        }
    }

    /**
     * Returns the stream that {@code print}/{@code println} write to, {@link System#out} unless
     * redirected via {@link #setOut(PrintStream)}.
     *
     * @return the standard output of this interpreter
     */
    public PrintStream out() {
        return out;
    }

    /**
     * Redirects the interpreter's standard output. Must be called before execution starts.
     *
     * @param out the stream to receive {@code print}/{@code println} output
     */
    public void setOut(PrintStream out) {
        this.out = out;
    }

    /**
     * Returns the interpreter's error stream, {@link System#err} unless redirected via
     * {@link #setErr(PrintStream)}.
     *
     * @return the standard error of this interpreter
     */
    public PrintStream err() {
        return err;
    }

    /**
     * Redirects the interpreter's error stream. Must be called before execution starts.
     *
     * @param err the stream to receive error output
     */
    public void setErr(PrintStream err) {
        this.err = err;
    }

    /**
     * Requests cooperative termination of every thread of this interpreter, including the main
     * interpreter thread.
     * <p>
     * Each registered {@link ThreadContext} gets its abort flag set and its thread interrupted,
     * so blocking operations (sleep, channel reads, I/O) return immediately and the next command
     * on each thread raises an {@link ch.turic.exceptions.ExecutionAborted}. Turicum code cannot
     * catch or suppress the abort; {@code finally}/exit blocks still get their bounded
     * {@link Grace} allowance.
     * <p>
     * This method is safe to call from any thread, typically from an embedder's watchdog timer.
     */
    public void abortAll() {
        contexts.keySet().forEach(ThreadContext::abort);
    }

}
