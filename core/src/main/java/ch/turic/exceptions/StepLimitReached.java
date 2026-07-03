package ch.turic.exceptions;

/**
 * Thrown when a thread-level or global step limit is exhausted.
 * Not catchable by Turicum {@code try}/{@code catch}; see {@link InterpreterHalt}.
 */
public final class StepLimitReached extends InterpreterHalt {
    public StepLimitReached(int stepLimit) {
        super("Step limit %d reached".formatted(stepLimit));
    }
}
