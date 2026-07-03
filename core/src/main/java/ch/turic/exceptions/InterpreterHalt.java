package ch.turic.exceptions;

/**
 * Terminates the execution of an interpreter thread in a way that Turicum code cannot catch.
 * <p>
 * Language-level {@code try}/{@code catch} handles {@link ExecutionException} only, so exceptions
 * of this type unwind the whole interpreter stack regardless of any {@code catch} blocks in the
 * executed program. This is the mechanism behind enforcement stops: the step limit and the abort
 * request. The guarded program must not be able to veto its own termination.
 * <p>
 * Host-side entry points (e.g. {@link ch.turic.Interpreter#execute}) convert this into an
 * {@link ExecutionException} at the boundary, so embedders still receive the documented
 * exception type.
 */
public sealed class InterpreterHalt extends RuntimeException permits StepLimitReached, ExecutionAborted {
    public InterpreterHalt(String message) {
        super(message);
    }
}
