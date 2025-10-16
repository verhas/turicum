package ch.turic.commands.operators;

import ch.turic.Command;
import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.LocalContext;

/**
 * The Fallible operator (?) provides safe navigation and error handling capabilities.
 * It allows for graceful handling of null values and exceptions in expression chains.
 */
@Operator.Symbol("?")
public class Fallible extends AbstractOperator {

    /**
     * Performs the unary operation of the fallible operator.
     * Simply returns the operand without modification.
     *
     * @param ctx The execution context
     * @param op  The operand to process
     * @return The unmodified operand
     * @throws ExecutionException If execution fails
     */
    @Override
    public Object unaryOp(LocalContext ctx, Object op) throws ExecutionException {
        return op;
    }

    /**
     * Handles exceptions that occur during execution.
     * Returns null to allow for safe navigation when errors occur.
     *
     * @param ctx   The execution context
     * @param t     The exception that occurred
     * @param right The right-hand command
     * @return null to indicate safe failure
     * @throws ExecutionException If exception handling fails
     */
    @Override
    public Object exceptionHandler(LocalContext ctx, ExecutionException t, Command right) throws ExecutionException {
        return null;
    }

    /**
     * Binary operation is not supported for the fallible operator.
     * Always throws an ExecutionException.
     *
     * @param ctx   The execution context
     * @param op1   The left operand
     * @param right The right command
     * @return Never returns as it always throws an exception
     * @throws ExecutionException Always throws this exception as binary operation is not supported
     */
    @Override
    public Object binaryOp(LocalContext ctx, Object op1, Command right) throws ExecutionException {
        throw new ExecutionException("Somehow '?' is used as a binary operator");
    }
}
