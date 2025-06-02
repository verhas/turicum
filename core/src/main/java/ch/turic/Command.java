package ch.turic;


import ch.turic.memory.Context;

/**
 * A command that the interpreter can execute
 */
public interface Command {
    /**
     * Execute the command.
     *
     * @param context the Jamal processing environment
     * @return the result of the execution
     */
    Object execute(Context context) throws ExecutionException;
}
