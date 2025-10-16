package ch.turic.commands;

import ch.turic.Command;
import ch.turic.exceptions.ExecutionException;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.LocalContext;
import ch.turic.utils.Unmarshaller;

public class DieCommand extends AbstractCommand {
    final Command expression;
    final Command condition;

    public Command expression() {
        return expression;
    }

    public Command condition() {
        return condition;
    }

    public static DieCommand factory(final Unmarshaller.Args args) {
        return new DieCommand(args.command("expression"), args.command("condition"));
    }

    public DieCommand(Command expression, Command condition) {
        this.expression = expression;
        this.condition = condition;
    }

    @Override
    public Conditional _execute(LocalContext context) throws ExecutionException {
        if (Cast.toBoolean(condition.execute(context))) {
            if (expression == null) {
                throw new ExecutionException("");
            } else {
                final var exception = expression.execute(context);
                if (exception != null) {
                    throw new ExecutionException(exception.toString());
                } else {
                    throw new ExecutionException("none");
                }
            }
        } else {
            return null;
        }
    }
}
