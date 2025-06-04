package ch.turic.commands;


import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.commands.operators.Operator;
import ch.turic.memory.Context;
import ch.turic.memory.LeftValue;
import ch.turic.utils.Unmarshaller;

import java.util.function.Function;

public class Assignment extends AbstractCommand {
    final LeftValue leftValue;
    final Command expression;
    final String op;

    public Command expression() {
        return expression;
    }

    public LeftValue leftValue() {
        return leftValue;
    }

    public static Assignment factory(Unmarshaller.Args args) {
        return new Assignment(args.get("leftValue", LeftValue.class),
                args.str("op"),
                args.command("expression"));
    }

    public Assignment(LeftValue leftValue, String op, Command expression) {
        this.expression = expression;
        this.op = op;
        this.leftValue = leftValue;
    }

    @Override
    public Object _execute(final Context ctx) throws ExecutionException {
        ctx.step();
        if (op.isEmpty()) {

            final var value = expression.execute(ctx);
            leftValue.assign(ctx, value);
            return value;
        }
        return leftValue.reassign(ctx, getOperation(op,ctx));
    }

    Function<Object, Object> getOperation(String operator, Context ctx) {
        if (Operator.OPERATORS.containsKey(operator)) {
            Operator op = Operator.OPERATORS.get(operator);
            return (oldValue) -> op.execute(ctx, context -> oldValue, expression);
        }
        throw new ExecutionException("Unknown operator " + operator);
    }

}
