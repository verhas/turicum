package ch.turic.exceptions;

/**
 * Thrown when {@link ch.turic.memory.ThreadContext#abort()} has requested the cooperative
 * termination of the thread. Not catchable by Turicum {@code try}/{@code catch};
 * see {@link InterpreterHalt}.
 */
public final class ExecutionAborted extends InterpreterHalt {
    public ExecutionAborted() {
        super("Execution aborted");
    }
}
