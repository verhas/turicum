package ch.turic.commands;

import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.memory.Context;
import ch.turic.utils.Unmarshaller;

public class ContinueCommand extends AbstractCommand {
    final Command expression;
    final Command condition;

    public Command expression() {
        return expression;
    }

    public Command condition() {
        return condition;
    }

    public ContinueCommand(Command expression, Command condition) {
        this.expression = expression;
        this.condition = condition;
    }

    public static ContinueCommand factory(final Unmarshaller.Args args) {
        return new ContinueCommand(
                args.command("expression"),
                args.command("condition")
        );
    }

    @Override
    public Conditional _execute(Context context) throws ExecutionException {
        throw new ExecutionException("Continue executed in non-loop.");
    }
}
