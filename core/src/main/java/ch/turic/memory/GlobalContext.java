package ch.turic.memory;

import ch.turic.Capability;
import ch.turic.TuriClass;
import ch.turic.exceptions.ExecutionException;
import ch.turic.exceptions.StepLimitReached;
import ch.turic.memory.debugger.DebuggerContext;
import ch.turic.utils.TuricumClassLoader;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
    // in-flight async task / flow cell futures; registered synchronously by the spawning thread,
    // so every acquired thread permit has a registered future before the spawn call returns.
    // Each future completes only after the task's finally block released its permit.
    private final Set<CompletableFuture<?>> tasks = ConcurrentHashMap.newKeySet();
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
    // null means full trust: every built-in is registered. A non-null set (even empty) means
    // sandboxed: only built-ins whose required capabilities are all granted are registered.
    private volatile Set<Capability> grantedCapabilities = null;
    // when non-null, file-reading built-ins resolve imports strictly under this root
    private volatile Path importRoot = null;
    // read-only and read-write file root sets; file built-ins confine against them (see
    // ch.turic.builtins.functions.fileio.SafePath). Both empty means unconfined file access.
    private volatile List<Path> fileReadRoots = List.of();
    private volatile List<Path> fileReadWriteRoots = List.of();
    // cap on the running total of live memory-mapped bytes; negative means no limit
    private volatile long maxMappedBytes = -1;
    private final AtomicLong mappedBytes = new AtomicLong();
    // per-session scratch directory for tmp_file()/tmp_dir(); created lazily, acts as an
    // additional read-write file root, deleted by closeFileResources()
    private volatile Path tempRoot = null;
    // open file handles (readers, writers, channels, mappings) force-closed at session end so
    // a script that never closes them cannot leak host file descriptors past the session
    private final Set<AutoCloseable> closeables = ConcurrentHashMap.newKeySet();

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
                // abort even when the task has not registered its thread yet: the abort flag is
                // checked on the task's first command, so a late-starting task exits immediately
                if (k.getThread() != Thread.currentThread()) {
                    k.abort();
                }
                if (k.getThread() != null && k.getThread() != Thread.currentThread()) {
                    k.getThread().join();
                }

            } catch (InterruptedException ignored) {
            }
        });
        // wait for the tasks whose thread was not registered when the loop above ran; a task
        // future completes only after its finally block returned the thread permit, so after
        // this loop every permit of this interpreter is back in the pool
        for (final var task : tasks.toArray(CompletableFuture<?>[]::new)) {
            try {
                task.join();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Registers an in-flight asynchronous task (an {@code async} block or a flow cell) so that
     * {@link #joinThreads()} can wait for it even when the task has not started running — and
     * thus has not registered its thread — yet. Must be called on the spawning thread, right
     * after the task was submitted, so that a thread permit acquired for the task always has a
     * registered future before the spawning call returns.
     *
     * @param task the future of the submitted task
     */
    public void registerTask(CompletableFuture<?> task) {
        tasks.add(task);
        task.whenComplete((result, throwable) -> tasks.remove(task));
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
     * The capabilities granted to this interpreter, or {@code null} for full trust (every
     * built-in registered). A built-in is registered only when this set contains every
     * capability it requires; see {@code BuiltIns.register}.
     *
     * @return the granted capability set, or {@code null} for full trust
     */
    public Set<Capability> grantedCapabilities() {
        return grantedCapabilities;
    }

    /**
     * Restricts this interpreter to the given capabilities. Must be set before built-ins are
     * registered. {@code null} (the default) means full trust.
     *
     * @param grantedCapabilities the capabilities to grant, or {@code null} for full trust
     */
    public void setGrantedCapabilities(Set<Capability> grantedCapabilities) {
        this.grantedCapabilities = grantedCapabilities;
    }

    /**
     * Returns {@code true} if the given built-in may be registered under the current grant.
     * A full-trust interpreter admits every built-in; a sandboxed one admits a built-in only
     * when all of its {@linkplain ch.turic.ServiceLoaded#capabilities() required capabilities}
     * are granted.
     *
     * @param required the capabilities a built-in requires
     * @return whether the built-in may be registered
     */
    public boolean capabilitiesGranted(Set<Capability> required) {
        return grantedCapabilities == null || grantedCapabilities.containsAll(required);
    }

    /**
     * @return the root directory under which file-reading built-ins must resolve imports, or
     * {@code null} when imports are not restricted to a root
     */
    public Path importRoot() {
        return importRoot;
    }

    /**
     * Restricts import resolution to the given root directory (see the embedding
     * documentation on file access scoping). {@code null} (the default) does not restrict.
     *
     * @param importRoot the root directory, or {@code null} for no restriction
     */
    public void setImportRoot(Path importRoot) {
        this.importRoot = importRoot;
    }

    /**
     * @return the read-only file root directories the file built-ins may read under; possibly
     * empty. The temp scratch directory ({@link #tempRoot()}) is <em>not</em> included; the
     * confinement helper adds it separately.
     */
    public List<Path> fileReadRoots() {
        return fileReadRoots;
    }

    /**
     * @return the read-write file root directories the file built-ins may read and modify
     * under; possibly empty
     */
    public List<Path> fileReadWriteRoots() {
        return fileReadWriteRoots;
    }

    /**
     * Sets the read-only file roots. Must be called before execution starts.
     *
     * @param fileReadRoots the roots; never {@code null}, possibly empty
     */
    public void setFileReadRoots(List<Path> fileReadRoots) {
        this.fileReadRoots = List.copyOf(fileReadRoots);
    }

    /**
     * Sets the read-write file roots. Must be called before execution starts.
     *
     * @param fileReadWriteRoots the roots; never {@code null}, possibly empty
     */
    public void setFileReadWriteRoots(List<Path> fileReadWriteRoots) {
        this.fileReadWriteRoots = List.copyOf(fileReadWriteRoots);
    }

    /**
     * @return the cap on the running total of live memory-mapped bytes of this interpreter;
     * negative means no limit
     */
    public long maxMappedBytes() {
        return maxMappedBytes;
    }

    /**
     * Caps the running total of live memory-mapped bytes. Must be called before execution
     * starts. Negative (the default) means no limit.
     *
     * @param maxMappedBytes the cap, {@code 0} to forbid mappings, negative for no limit
     */
    public void setMaxMappedBytes(long maxMappedBytes) {
        this.maxMappedBytes = maxMappedBytes;
    }

    /**
     * Reserves {@code n} bytes of memory-mapping budget, failing when the
     * {@link #maxMappedBytes()} cap would be exceeded. Balanced by
     * {@link #releaseMappedBytes(long)} when the mapping handle is closed.
     *
     * @param n the length of the mapping about to be created
     * @throws ExecutionException if the cap would be exceeded
     */
    public void reserveMappedBytes(long n) throws ExecutionException {
        final var cap = maxMappedBytes;
        final var total = mappedBytes.addAndGet(n);
        if (cap >= 0 && total > cap) {
            mappedBytes.addAndGet(-n);
            throw new ExecutionException(
                    "Mapping %d bytes would exceed the sandbox limit of %d total mapped bytes (%d already mapped)",
                    n, cap, total - n);
        }
    }

    /**
     * Returns {@code n} bytes of memory-mapping budget reserved by
     * {@link #reserveMappedBytes(long)}.
     *
     * @param n the length of the mapping that was closed
     */
    public void releaseMappedBytes(long n) {
        mappedBytes.addAndGet(-n);
    }

    /**
     * Returns the per-session temp scratch directory, creating it on first call. The scratch
     * directory backs the {@code tmp_file()}/{@code tmp_dir()} built-ins and acts as an
     * additional read-write file root; {@link #closeFileResources()} deletes it recursively.
     *
     * @return the scratch directory
     * @throws ExecutionException if the directory cannot be created
     */
    public Path tempRoot() throws ExecutionException {
        var root = tempRoot;
        if (root == null) {
            synchronized (this) {
                root = tempRoot;
                if (root == null) {
                    try {
                        root = Files.createTempDirectory("turi-scratch-").toRealPath();
                    } catch (IOException e) {
                        throw new ExecutionException(e, "Cannot create the session temp directory: " + e.getMessage());
                    }
                    tempRoot = root;
                }
            }
        }
        return root;
    }

    /**
     * @return the per-session temp scratch directory, or {@code null} when no temp file has
     * been created yet; unlike {@link #tempRoot()} this never creates the directory
     */
    public Path tempRootIfCreated() {
        return tempRoot;
    }

    /**
     * Registers an open file resource (stream handle, channel, mapping) so that
     * {@link #closeFileResources()} can force-close it when the interpreter or session ends —
     * a script that opens files in a loop and never closes them must not leak host file
     * descriptors past the session. The handle unregisters itself via
     * {@link #unregisterCloseable(AutoCloseable)} when it is closed by the script.
     *
     * @param closeable the open resource
     */
    public void registerCloseable(AutoCloseable closeable) {
        closeables.add(closeable);
    }

    /**
     * Removes a resource registered with {@link #registerCloseable(AutoCloseable)}, typically
     * because the script closed it in an orderly way.
     *
     * @param closeable the resource that was closed
     */
    public void unregisterCloseable(AutoCloseable closeable) {
        closeables.remove(closeable);
    }

    /**
     * Force-closes every file resource still registered and deletes the temp scratch
     * directory, if one was created. Called when the interpreter or session is closed;
     * exceptions from individual resources are suppressed — this is last-resort cleanup, the
     * orderly path is the script's own {@code close()}/{@code with} handling.
     */
    public void closeFileResources() {
        for (final var closeable : closeables.toArray(AutoCloseable[]::new)) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
            closeables.remove(closeable);
        }
        final var root = tempRoot;
        if (root != null) {
            tempRoot = null;
            try (final var walk = Files.walk(root)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                    }
                });
            } catch (IOException ignored) {
            }
        }
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
