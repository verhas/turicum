package ch.turic.commands;


import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.commands.operators.Operator;
import ch.turic.memory.Context;
import ch.turic.utils.Unmarshaller;

import java.util.Objects;

public class Operation extends AbstractCommand {
    final String operator;
    final Command left;
    final Command right;

    public Command left() {
        return left;
    }

    public String operator() {
        return operator;
    }

    public Command right() {
        return right;
    }

    public Operation(String operator, Command left, Command right) {
        Objects.requireNonNull(operator);
        Objects.requireNonNull(right);
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    public static Operation factory(final Unmarshaller.Args args) {
        return new Operation(
                args.str("operator"),
                args.command("left"),
                args.command("right")
        );
    }


    @Override
    public Object _execute(final Context ctx) throws ExecutionException {
        ctx.step();

        if (Operator.OPERATORS.containsKey(operator)) {
            Operator op = Operator.OPERATORS.get(operator);
            return op.execute(ctx, left, right);
        }
        throw new ExecutionException("Unknown operator " + operator);
    }

}
