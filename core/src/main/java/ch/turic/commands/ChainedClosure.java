package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.LngCallable;
import ch.turic.memory.Context;
import ch.turic.memory.HasFields;
import ch.turic.memory.LngObject;
import ch.turic.memory.Variable;
import ch.turic.utils.NullableOptional;
import ch.turic.utils.Unmarshaller;

import java.util.Arrays;
import java.util.SequencedMap;

/**
 * Represents a closure that chains the execution of two {@link ClosureOrMacro} objects.
 * Allows their sequential execution in a provided context.
 * <p>
 * This class extends {@link AbstractCommand} and implements {@link ClosureOrMacro} and
 * {@link LngCallable.LngCallableClosure}, making it highly versatile for handling chained
 * closure executions as commands or callable objects.
 * <p>
 * The first closure in the chain executes first, and its context is passed to the second
 * closure for execution.
 */
public final class ChainedClosure extends AbstractCommand implements ClosureOrMacro, LngCallable.LngCallableClosure {
    final ClosureOrMacro closure1;
    final ClosureOrMacro closure2;

    public ChainedClosure(ClosureOrMacro closure1, ClosureOrMacro closure2) {
        this.closure1 = closure1;
        this.closure2 = closure2;
    }

    public static ChainedClosure factory(final Unmarshaller.Args args) {
        throw new ExecutionException("Chained closures can only be dynamically created.");
    }

    @Override
    public String name() {
        return closure1.name() + "##" + closure2.name() ;
    }

    public ParameterList parameters() {
        return closure1.parameters();
    }

    public String[] returnType() {
        return closure2.returnType();
    }

    @Override
    public Context wrapped() {
        return closure1.wrapped();
    }

    @Override
    public Object _execute(final Context ctx) throws ExecutionException {
        ctx.step();
        return new FunctionCall((c) -> closure2, new FunctionCall.Argument[]{new FunctionCall.Argument(null, closure1)}).execute(ctx);
    }

    @Override
    public Object call(final ch.turic.Context callerContext, final Object... arguments) {
        if (!(callerContext instanceof Context context)) {
            throw new RuntimeException("Cannot work with this context implementation. This is an internal error.");
        }
        final var ctx = context.wrap(this.closure1.wrapped());
        FunctionCall.defineArgumentsInContext(ctx, context, closure1.parameters(), Arrays.stream(arguments)
                .map(param -> new FunctionCall.ArgumentEvaluated(null, param)).
                toArray(FunctionCall.ArgumentEvaluated[]::new), true);
        return execute(ctx);
    }

    @Override
    public NullableOptional<Object> methodCall(Context context, HasFields obj, String methodName, FunctionCall.Argument[] arguments) {
        final var argValues = evaluateArguments(context, arguments);
        return ClosureOrMacro.callTheMethod(context, obj, methodName, argValues, this);
    }

    @Override
    public FunctionCall.ArgumentEvaluated[] evaluateArguments(Context context, FunctionCall.Argument[] arguments) {
        return Closure.evaluateClosureArguments(context, arguments);
    }
}
