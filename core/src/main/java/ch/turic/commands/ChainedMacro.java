package ch.turic.commands;

import ch.turic.LngCallable;
import ch.turic.memory.Context;

import java.util.Arrays;

/**
 * Represents a closure that chains the execution of two {@link ClosureLike} objects.
 * Allows their sequential execution in a provided context.
 * <p>
 * This class extends {@link AbstractCommand} and implements {@link ClosureLike} and
 * {@link LngCallableClosure}, making it highly versatile for handling chained
 * closure executions as commands or callable objects.
 * <p>
 * The first closure in the chain executes first, and its context is passed to the second
 * closure for execution.
 */
public final class ChainedMacro extends ChainedClosureOrMacro implements LngCallable.LngCallableClosure {

    public ChainedMacro(ClosureLike closure1, ClosureLike closure2) {
        super(closure1, closure2);
    }

    @Override
    public FunctionCall.ArgumentEvaluated[] evaluateArguments(Context context, FunctionCall.Argument[] arguments) {
        return Macro.evaluateMacroArguments(context, arguments);
    }
}
