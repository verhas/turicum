package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.LngCallable;
import ch.turic.memory.Context;
import ch.turic.memory.HasFields;
import ch.turic.utils.NullableOptional;
import ch.turic.utils.Unmarshaller;

import java.util.Arrays;

public abstract sealed class ChainedClosureOrMacro extends ClosureLike implements LngCallable.LngCallableClosure permits ChainedClosure, ChainedMacro{
    final private ClosureLike closure1;
    final private ClosureLike closure2;

    public ClosureLike getClosure1() {
        return closure1;
    }

    public ClosureLike getClosure2() {
        return closure2;
    }

    public ChainedClosureOrMacro(ClosureLike closure1, ClosureLike closure2) {
        this.closure1 = closure1;
        this.closure2 = closure2;
    }

    public static ChainedClosureOrMacro factory(final Unmarshaller.Args args) {
        throw new ExecutionException("Chained closures and macros can only be dynamically created.");
    }

    @Override
    public String name() {
        return closure1.name() + "##" + closure2.name();
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
    public void setCurriedArguments(FunctionCallOrCurry.ArgumentEvaluated[] curriedArguments) {
        closure1.setCurriedArguments(curriedArguments);
    }

    @Override
    public void setCurriedSelf(HasFields curriedSelf) {
        closure1.setCurriedSelf(curriedSelf);
    }

    public FunctionCallOrCurry.ArgumentEvaluated[] getCurriedArguments() {
        return closure1.getCurriedArguments();
    }

    public HasFields getCurriedSelf() {
        return closure1.getCurriedSelf();
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
        return ClosureLike.callTheMethod(context, obj, methodName, argValues, this);
    }

    @Override
    public FunctionCall.ArgumentEvaluated[] evaluateArguments(Context context, FunctionCall.Argument[] arguments) {
        return Closure.evaluateClosureArguments(context, arguments);
    }
}
