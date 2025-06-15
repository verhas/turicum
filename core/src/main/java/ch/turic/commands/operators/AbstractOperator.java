package ch.turic.commands.operators;

import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.commands.*;
import ch.turic.memory.Context;
import ch.turic.memory.LngObject;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public abstract class AbstractOperator implements Operator {

    /**
     * Executes the operator with the provided operands in the given context.
     *
     * If the left operand is null, treats the operation as unary and attempts to dispatch to a corresponding operator method on the right operand if it is an object; otherwise, delegates to {@code unaryOp}. If the left operand is not null, treats the operation as binary and attempts to dispatch to a corresponding operator method on the left operand if it is an object; otherwise, delegates to {@code binaryOp}.
     *
     * Handles context shadowing and exception propagation. Operator methods must accept exactly one argument.
     *
     * @param context the execution context
     * @param left the left operand command, or null for unary operations
     * @param right the right operand command
     * @return the result of the operator execution
     * @throws ExecutionException if execution fails or operator dispatch is invalid
     */
    @Override
    public final Object execute(Context context, Command left, Command right) throws ExecutionException {
        if (left == null) {
            final Object op2;
            final var shadowed = context.shadow();
            try {
                op2 = right.execute(shadowed);
                TryCatch.exportFromTemporaryContext(shadowed, context);
            } catch (ExecutionException e) {
                return exceptionHandler(shadowed, e, right);
            }
            if (!(op2 instanceof LngObject lngObject)) {
                return unaryOp(context, op2);
            }
            final var operatorMethod = lngObject.getField(symbol());
            if (operatorMethod == null) {
                return unaryOp(context, op2);
            }
            if (operatorMethod instanceof ClosureOrMacro command) {
                ExecutionException.when(command.parameters().doesNotFitOperator(), "Operator methods must have exactly one argument");
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

        final Object op1;
        final var shadowed = context.shadow();
        try {
            op1 = left.execute(shadowed);
            TryCatch.exportFromTemporaryContext(shadowed, context);
        } catch (ExecutionException e) {
            return exceptionHandler(shadowed, e, right);
        }

        if (!(op1 instanceof LngObject lngObject)) {
            return binaryOp(context, op1, right);
        }
        final var operatorMethod = lngObject.getField(symbol());
        if (operatorMethod == null) {
            return binaryOp(context, op1, right);
        }
        if (operatorMethod instanceof ClosureOrMacro command) {
            ExecutionException.when(command.parameters().doesNotFitOperator(), "Operator methods must have exactly one argument");
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

    public Object exceptionHandler(Context ctx, ExecutionException t, Command right) throws ExecutionException {
        throw t;
    }

    /**
     * Performs a binary operation on two operands, supporting both primitive and reflective invocation.
     *
     * If both operands are longs and a long operation is provided, applies it. If either operand is a double and a double operation is provided, applies it. Otherwise, attempts to invoke a binary method reflectively using the provided operator name. Throws an exception if operands are null or if no suitable operation is found.
     *
     * @param name the operator method name for reflective lookup
     * @param op1 the left operand
     * @param op2 the right operand
     * @param longOp the operation to apply if both operands are longs
     * @param doubleOp the operation to apply if either operand is a double
     * @return the result of the binary operation
     * @throws ExecutionException if operands are null or no suitable operation is found
     */
    protected Object binary(final String name,
                            final Object op1, final Object op2,
                            final BiFunction<Long, Long, Long> longOp,
                            final BiFunction<Double, Double, Double> doubleOp
    ) throws ExecutionException {
        final Supplier<String> symbol = () -> this.getClass().getAnnotation(Symbol.class).value();
        ExecutionException.when(op1 == null || op2 == null, "You cannot '%s' on undefined value", symbol.get());
        if (longOp != null && Cast.isLong(op1) && Cast.isLong(op2)) {
            return longOp.apply(Cast.toLong(op1), Cast.toLong(op2));
        }
        if (doubleOp != null && (Cast.isDouble(op1) || Cast.isDouble(op2))) {
            return doubleOp.apply(Cast.toDouble(op1), Cast.toDouble(op2));
        }

        return Reflect.getBinaryMethod(name, op1, op2).map(Reflect.Op::callMethod)
                .orElseThrow(() -> new ExecutionException("Cannot calculate '%s' %s '%s'", op1, symbol.get(), op2));
    }
}

