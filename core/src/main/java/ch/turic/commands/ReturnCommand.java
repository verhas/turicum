package ch.turic.commands;

import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.LocalContext;
import ch.turic.utils.Unmarshaller;

public class ReturnCommand extends AbstractCommand {
    final Command expression;
    final Command condition;

    public Command condition() {
        return condition;
    }

    public Command expression() {
        return expression;
    }

    public static ReturnCommand factory(final Unmarshaller.Args args) {
        return new ReturnCommand(
                args.command("expression"),
                args.command("condition")
        );
    }

    public ReturnCommand(Command expression, Command condition) {
        this.condition = condition;
        this.expression = expression;
    }

    @Override
    public Conditional _execute(LocalContext context) throws ExecutionException {
        if (Cast.toBoolean(condition.execute(context))) {
            if (expression == null) {
                return Conditional.doReturn(null);
            } else {
                return Conditional.doReturn(expression.execute(context));
            }
        } else {
            return Conditional.result(null);
        }
    }
}
