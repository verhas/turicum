package ch.turic.memory;

import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.debugger.DebuggerContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Holds all per-thread execution state for a single interpreter thread.
 * <p>
 * Each virtual thread that executes Turicum code has exactly one {@code ThreadContext}.
 * It is shared by all {@link LocalContext} frames that live on the same thread — frames
 * created via {@link LocalContext#wrap()} inherit the context of their parent and therefore
 * share the same {@code ThreadContext} without registering it again.
 * <p>
 * The thread context owns:
 * <ul>
 *   <li>The Turicum call-stack trace ({@link LngStackFrame} list), used for error reporting.</li>
 *   <li>The thread-level step counter and step limit, which can independently cap execution
 *       in addition to the global step limit maintained by {@link GlobalContext}.</li>
 *   <li>An optional {@link Yielder} that connects the thread to an asynchronous parent
 *       via bidirectional {@link Channel channels}.</li>
 *   <li>An optional {@link DebuggerContext} that controls single-stepping and breakpoints
 *       for this specific thread.</li>
 *   <li>An abort flag that can be set from any other thread to request cooperative
 *       termination.</li>
 * </ul>
 */
public class ThreadContext {
    private Yielder yielder = null;
    private volatile boolean aborted = false;

    /**
     * Requests cooperative termination of this thread.
     * <p>
     * Sets the {@code aborted} flag to {@code true} so that the next call to
     * {@link ch.turic.commands.AbstractCommand#execute} will throw a {@link RuntimeException} and
     * unwind the interpreter stack. If a {@link Thread} has been associated with
     * this context via {@link #setThread(Thread)}, it is also interrupted so that
     * any blocking I/O or {@link java.util.concurrent.BlockingQueue} operation
     * returns immediately.
     */
    public void abort() {
        aborted = true;
        if (thread != null) {
            thread.interrupt();
        }
    }

    /**
     * Returns {@code true} if {@link #abort()} has been called on this context.
     * <p>
     * The interpreter checks this flag at the start of every command execution and
     * raises a {@link RuntimeException} to stop evaluation as soon as possible.
     *
     * @return {@code true} if abortion has been requested
     */
    public boolean isAborted() {
        return aborted;
    }

    private Thread thread;

    /**
     * Returns the Java thread currently executing inside this context, or {@code null}
     * if the thread has not yet been set.
     * <p>
     * The thread reference is set from inside the running thread via
     * {@link #setThread(Thread)} once it starts, because virtual threads do not have a
     * stable identity before they begin executing their first task.
     *
     * @return the associated {@link Thread}, or {@code null}
     */
    public Thread getThread() {
        return thread;
    }

    /**
     * Associates a Java thread with this context.
     * <p>
     * Called at the start of each virtual-thread task so that {@link #abort()} can
     * interrupt the correct thread. May also be called from outside the thread before
     * execution begins when the thread identity is known in advance.
     *
     * @param thread the thread to associate with this context
     */
    public void setThread(Thread thread) {
        this.thread = thread;
    }

    private DebuggerContext debuggerContext;

    /**
     * Returns the debugger context for this thread, or {@code null} if debugging is
     * not active on this thread.
     * <p>
     * When non-null, {@link ch.turic.commands.AbstractCommand#execute} consults this context to handle
     * breakpoints and single-step commands before every statement.
     *
     * @return the per-thread {@link DebuggerContext}, or {@code null}
     */
    public DebuggerContext getDebuggerContext() {
        return debuggerContext;
    }

    /**
     * Attaches or replaces the debugger context for this thread.
     * <p>
     * Pass {@code null} to disable debugging on this thread.
     *
     * @param debuggerContext the new {@link DebuggerContext}, or {@code null}
     */
    public void setDebuggerContext(DebuggerContext debuggerContext) {
        this.debuggerContext = debuggerContext;
    }

    private final List<LngStackFrame> trace = new ArrayList<>();

    /**
     * Creates a thread context that is not yet associated with any Java thread.
     * <p>
     * Used when the thread context must be created before the virtual thread starts
     * (for example, to copy parent variables into the new context before submitting
     * the task to an executor). The thread itself must call {@link #setThread(Thread)}
     * once it begins running.
     */
    public ThreadContext() {
    }

    /**
     * Creates a thread context already associated with the given Java thread.
     * <p>
     * Typically used for the main interpreter thread, whose identity is known before
     * the first command is executed.
     *
     * @param thread the thread that will execute inside this context
     */
    public ThreadContext(Thread thread) {
        this.thread = thread;
    }

    /**
     * Returns the current depth of the Turicum call-stack trace.
     * <p>
     * Used by {@link ch.turic.commands.TryCatch} to record the trace depth at the
     * point where a {@code try} block begins, so that {@link #resetTrace(int)} can
     * later trim back to that depth when an exception is caught.
     *
     * @return number of frames currently on the trace stack
     */
    public int traceSize() {
        return trace.size();
    }

    /**
     * Trims the call-stack trace to the given depth.
     * <p>
     * Called by {@link ch.turic.commands.TryCatch} when an exception is caught.
     * All frames deeper than {@code size} are discarded, leaving only those frames
     * that are still valid at the level where the {@code catch} block begins.
     * This prevents stale inner frames from appearing in the error trace after
     * exception handling has restored control to an outer scope.
     *
     * @param size the number of frames to keep; frames at index {@code >= size} are removed
     */
    public void resetTrace(int size) {
        final var safe = getStackTrace();
        trace.clear();
        for (int i = 0; i < size; i++) {
            trace.add(safe.get(i));
        }
    }

    /**
     * Returns a snapshot of the current Turicum call-stack trace.
     * <p>
     * The returned list is a defensive copy; modifications to it do not affect the
     * live trace.
     *
     * @return an ordered list of {@link LngStackFrame} objects, outermost frame first
     */
    public List<LngStackFrame> getStackTrace() {
        return new ArrayList<>(trace);
    }

    /**
     * Pushes a new frame onto the call-stack trace.
     * <p>
     * Called by {@link ch.turic.commands.AbstractCommand#execute} immediately before delegating to
     * {@link ch.turic.commands.AbstractCommand#_execute}.
     *
     * @param frame the stack frame representing the command about to be executed
     */
    public void push(LngStackFrame frame) {
        trace.add(frame);
    }

    /**
     * Pops the most recent frame from the call-stack trace.
     * <p>
     * Called by {@link ch.turic.commands.AbstractCommand#execute} after {@link ch.turic.commands.AbstractCommand#_execute}
     * returns normally.
     */
    public void pop() {
        trace.removeLast();
    }

    /**
     * Returns the {@link Yielder} registered for this thread, or {@code null} if no
     * asynchronous stream has been attached.
     * <p>
     * The yielder provides the two {@link Channel channels} — one from parent to child
     * and one from child to parent — through which the async block and its caller
     * exchange messages.
     *
     * @return the current {@link Yielder}, or {@code null}
     * @throws ExecutionException never thrown by this implementation; declared for
     *                            compatibility with override points
     */
    public Yielder yielder() throws ExecutionException {
        return yielder;
    }

    /**
     * Registers the single {@link Yielder} for this thread.
     * <p>
     * A thread context supports at most one yielder at a time. This method is called
     * by {@link ch.turic.commands.AsyncEvaluation} immediately after creating the
     * {@link AsyncStreamHandler} and before submitting the async task to the executor.
     * Calling it a second time replaces the previous yielder; in the current execution
     * model, each async thread is created fresh and therefore receives exactly one
     * yielder for its lifetime.
     *
     * @param yielder the {@link Yielder} to attach to this thread context
     * @throws ExecutionException never thrown by this implementation; declared for
     *                            compatibility with override points
     */
    public void addYielder(Yielder yielder) throws ExecutionException {
        this.yielder = yielder;
    }

    private int stepLimit = -1;
    private final AtomicInteger steps = new AtomicInteger();

    /**
     * Sets the maximum number of execution steps allowed for this thread.
     * <p>
     * Once the thread-level step counter reaches this value, {@link #step()} throws
     * an {@link ExecutionException}. A negative value (the default) means unlimited.
     * This limit operates independently of the global step limit stored in
     * {@link GlobalContext#stepLimit}; both are checked on every step.
     *
     * @param stepLimit the maximum permitted steps, or a negative value for no limit
     */
    public void setStepLimit(int stepLimit) {
        this.stepLimit = stepLimit;
    }

    /**
     * Increments the thread-level step counter and throws if the limit is reached.
     * <p>
     * Called by commands that represent a single unit of work (assignments, function
     * calls, loop iterations, etc.). If {@code stepLimit} is negative the check is
     * skipped entirely. Otherwise, an {@link ExecutionException} is thrown as soon
     * as the counter meets or exceeds {@code stepLimit}, stopping further execution
     * on this thread.
     *
     * @throws ExecutionException if the thread-level step limit has been reached
     */
    public void step() throws ExecutionException {
        final var currentStep = steps.incrementAndGet();
        if (stepLimit < 0) return;
        if (stepLimit <= currentStep) {
            throw new ExecutionException("Step limit %d reached", stepLimit);
        }
    }

}
