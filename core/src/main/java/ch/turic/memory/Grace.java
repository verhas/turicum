package ch.turic.memory;

import ch.turic.exceptions.ExecutionAborted;
import ch.turic.exceptions.InterpreterHalt;

/**
 * Bounded cleanup grace for a single thread: lets a finally/exit block release resources
 * after an {@link InterpreterHalt} (step limit or abort), without reopening the "a hostile
 * cleanup block hangs the thread forever" hole that motivated making halts uncatchable in
 * the first place.
 * <p>
 * The model is the same one process supervisors use for shutdown: the halt fires (the
 * equivalent of SIGTERM), one bounded, ONE-SHOT window is granted across the <em>whole</em>
 * unwind — not per {@code try}/{@code finally} frame; an outer cleanup block does not get a
 * second window just because an inner one already used the first — and once that budget is
 * spent the halt becomes final and unconditional (the equivalent of SIGKILL): every further
 * step or command execution throws immediately, forever, and no further grace can ever be
 * granted on this thread. The effective ceiling is therefore {@code stepLimit + steps},
 * fixed and known in advance — not a weaker guarantee than having no grace at all, just a
 * documented larger one.
 * <p>
 * With {@link #setSteps(int)} left at its default of 0 (disabled), {@link #beginCleanup()}
 * is always a no-op and {@link #isActive()}/{@link #isFinal()} are never true: zero behavior
 * change for callers that do not opt in.
 * <p>
 * One instance belongs to exactly one {@link ThreadContext}. See
 * {@code AbstractCommand.execute}, {@code LocalContext.step}, {@code TryCatch},
 * {@code WithCommand}, and {@code WhileLoop} for how it is consulted and armed.
 */
public final class Grace {
    /** Extra steps granted once a halt fires; 0 (the default) disables the feature. */
    private int steps = 0;
    /** {@code true} once ANY {@link InterpreterHalt} has been observed on this thread. */
    private volatile boolean triggered = false;
    /** One-shot latch: the window has been opened for this thread's current unwind. */
    private volatile boolean granted = false;
    /** Steps left in the window; meaningful only while {@link #granted} is true. */
    private volatile int remaining = 0;
    /** Once true, every further check throws immediately and unconditionally; never reset. */
    private volatile boolean exhausted = false;
    /** The first halt observed on this thread; reused (not rebuilt) for every later throw. */
    private volatile RuntimeException cause = null;

    /**
     * Sets the number of extra steps a finally/exit block is allowed to run after this
     * thread has been halted. 0 (the default) disables cleanup grace entirely.
     *
     * @param steps the grace budget in steps, or 0 to disable
     */
    public void setSteps(int steps) {
        this.steps = steps;
    }

    /**
     * Records the first {@link InterpreterHalt} observed on this thread. Idempotent: only
     * the first call has any effect, so it is safe to call on every halt, not just the
     * first one — later halts (e.g. a step limit re-tripping because it is never reset)
     * do not overwrite the originally captured cause.
     *
     * @param cause the halt that is about to be (re)thrown
     */
    public void noteHalt(RuntimeException cause) {
        triggered = true;
        if (this.cause == null) {
            this.cause = cause;
        }
    }

    /**
     * Opens a bounded cleanup window, if this thread is actually halting, grace is
     * configured, and a window has not already been granted during this unwind. Called by
     * the runtime immediately before running a finally/exit body; safe to call
     * unconditionally, including when no halt is in progress or grace is disabled — in
     * either case it is a no-op, so ordinary (non-halting) execution of a finally/exit
     * block is entirely unaffected.
     */
    public void beginCleanup() {
        if (exhausted || granted || steps <= 0 || !triggered) {
            return;
        }
        granted = true;
        remaining = steps;
    }

    /**
     * @return {@code true} if this thread is currently inside a granted, not-yet-exhausted
     * cleanup window
     */
    public boolean isActive() {
        return granted && remaining > 0;
    }

    /**
     * Consumes one unit of the budget.
     *
     * @return {@code true} if the caller may proceed with one more step of cleanup code;
     * {@code false} if the budget just ran out — the caller must halt immediately, via
     * {@link #finalCause()}, and must not attempt any further cleanup
     */
    public boolean consumeStep() {
        if (remaining <= 0) {
            exhausted = true;
            return false;
        }
        remaining--;
        return true;
    }

    /**
     * @return {@code true} if the halt on this thread is now final and unconditional: no
     * further code, including further cleanup blocks anywhere up the call stack, may run
     */
    public boolean isFinal() {
        return exhausted;
    }

    /**
     * Marks the halt as final (idempotent) and returns the exception to throw. Reuses the
     * originally captured cause rather than constructing a new one, so the message seen by
     * the program is always the reason it was halted in the first place, whether or not a
     * cleanup window was granted or exhausted along the way.
     *
     * @return the {@link InterpreterHalt} to throw, unconditionally and immediately
     */
    public RuntimeException finalCause() {
        exhausted = true;
        return cause != null ? cause : new ExecutionAborted();
    }
}
