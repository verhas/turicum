package ch.turic.commands;

import ch.turic.Command;
import ch.turic.exceptions.ExecutionException;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.LocalContext;
import ch.turic.memory.Sentinel;
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

    /**
     * Executes the command logic based on the given condition and returns a {@link Conditional} result.
     * If the condition evaluates to true, the method will determine whether to return a Conditional
     * with a value (based on the expression) or a default value indicating continuation. If the condition
     * evaluates to false, null is returned.
     *
     * @param context the execution context within which the command operates
     * @return a {@link Conditional} indicating a continuation with a result if the condition is true;
     *         null otherwise
     * @throws ExecutionException if an error occurs during the execution of the condition or expression
     */
    @Override
    public Conditional _execute(LocalContext context) throws ExecutionException {
        if (Cast.toBoolean(condition.execute(context))) {
            if (expression == null) {
                return Conditional.doContinue(Sentinel.NO_VALUE);
            } else {
                return Conditional.doContinue(expression.execute(context));
            }
        } else {
            return null;
        }
    }
}
