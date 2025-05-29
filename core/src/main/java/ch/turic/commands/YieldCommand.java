package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.Channel;
import ch.turic.memory.Context;
import ch.turic.utils.Unmarshaller;

public class YieldCommand extends AbstractCommand {
    final Command expression;
    final Command condition;

    public Command condition() {
        return condition;
    }

    public Command expression() {
        return expression;
    }

    public static YieldCommand factory(final Unmarshaller.Args args) {
        return new YieldCommand(
                args.command("expression"),
                args.command("condition")
        );
    }

    public YieldCommand(Command expression, Command condition) {
        this.expression = expression;
        this.condition = condition;
    }

    @Override
    public Object _execute(final Context context) throws ExecutionException {
        if( Cast.toBoolean(condition.execute(context))) {
            final var result = expression.execute(context);
            context.threadContext.yielder().toParent().send((Channel.Message)Channel.Message.of(result));
            return result;
        }else{
            return null;
        }
    }
}
