package ch.turic.commands.operators;

import ch.turic.ExecutionException;
import ch.turic.commands.*;
import ch.turic.memory.Context;
import ch.turic.memory.LngObject;

import java.util.function.BiFunction;

public abstract class AbstractOperator implements Operator {

    @Override
    public final Object execute(Context context, Command left, Command right) throws ExecutionException {
        if (left == null) {
            final var op2 = right.execute(context);
            if (!(op2 instanceof LngObject lngObject)) {
                return unaryOp(context, op2);
            }
            final var operatorMethod = lngObject.getField(symbol());
            if (operatorMethod == null) {
                return unaryOp(context, op2);
            }
            if (operatorMethod instanceof ClosureOrMacro command) {
                ExecutionException.when(!command.parameters().fitOperator(), "Operator methods must have exactly one argument");
                final var argValues = new FunctionCall.ArgumentEvaluated[]{new FunctionCall.ArgumentEvaluated(null, null)};
                final Context ctx;
                if (command.wrapped() == null) {
                    ctx = context.wrap(lngObject.context());
                } else {
                    ctx = context.wrap(command.wrapped());
                    ctx.let0("this", lngObject);
                }
                FunctionCall.freezeThisAndCls(ctx);
                FunctionCall.defineArgumentsInContext(ctx, context, command.parameters(), argValues, true);
                return command.execute(ctx);
            } else {
                throw new ExecutionException("You can not execute the operator " + operatorMethod + " on a " + op2);
            }
        }
        final var op1 = left.execute(context);
        if (!(op1 instanceof LngObject lngObject)) {
            return binaryOp(context, op1, right);
        }
        final var operatorMethod = lngObject.getField(symbol());
        if (operatorMethod == null) {
            return binaryOp(context, op1, right);
        }
        if (operatorMethod instanceof ClosureOrMacro command) {
            ExecutionException.when(!command.parameters().fitOperator(), "Operator methods must have exactly one argument");
            final var argValues = new FunctionCall.ArgumentEvaluated[]{
                    switch (command) {
                        case Closure ignored -> new FunctionCall.ArgumentEvaluated(null, right.execute(context));
                        case Macro ignored -> new FunctionCall.ArgumentEvaluated(null, right);
                    }
            };
            final Context ctx;
            if (command.wrapped() == null) {
                ctx = context.wrap(lngObject.context());
            } else {
                ctx = context.wrap(command.wrapped());
                ctx.let0("this", lngObject);
            }
            ctx.setCaller(context);
            FunctionCall.freezeThisAndCls(ctx);
            FunctionCall.defineArgumentsInContext(ctx, context, command.parameters(), argValues, true);
            return command.execute(ctx);
        }
        return binaryOp(context, op1, right);
    }

    public Object unaryOp(Context ctx, Object op) throws ExecutionException {
        throw new ExecutionException(symbol(), "is not an unary operator");
    }

    public abstract Object binaryOp(Context ctx, Object left, Command right) throws ExecutionException;

    protected Object binary(final String name,
                            final Object op1, final Object op2,
                            final BiFunction<Long, Long, Long> longOp,
                            final BiFunction<Double, Double, Double> doubleOp
    ) throws ExecutionException {
        ExecutionException.when(op1 == null || op2 == null, "You cannot " + name + " on undefined value");
        if (longOp != null && Cast.isLong(op1) && Cast.isLong(op2)) {
            return longOp.apply(Cast.toLong(op1), Cast.toLong(op2));
        }
        if (doubleOp != null && (Cast.isDouble(op1) || Cast.isDouble(op2))) {
            return doubleOp.apply(Cast.toDouble(op1), Cast.toDouble(op2));
        }

        return Reflect.getBinaryMethod(name, op1, op2).map(Reflect.Op::callMethod)
                .orElseThrow(() -> new ExecutionException("Cannot calculate '%s' %s '%s'", op1, name, op2));
    }
}

