package ch.turic.commands;


import ch.turic.ExecutionException;
import ch.turic.memory.Context;
import ch.turic.memory.LeftValue;
import ch.turic.utils.Unmarshaller;

public class Assignment extends AbstractCommand {
    final LeftValue leftValue;
    final Command expression;

    public Command expression() {
        return expression;
    }

    public LeftValue leftValue() {
        return leftValue;
    }

    public static Assignment factory(Unmarshaller.Args args) {
        return new Assignment(args.get("leftValue",LeftValue.class),
                args.command("expression"));
    }

    public Assignment(LeftValue leftValue, Command expression) {
        this.expression = expression;
        this.leftValue = leftValue;
    }

    @Override
    public Object _execute(final Context ctx) throws ExecutionException {
        ctx.step();
        final var value = expression.execute(ctx);
        leftValue.assign(ctx, value);
        return value;
    }
}
