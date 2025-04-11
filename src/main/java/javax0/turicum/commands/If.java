package javax0.turicum.commands;


import javax0.turicum.ExecutionException;
import javax0.turicum.commands.operators.Cast;
import javax0.turicum.memory.Context;

public class If extends AbstractCommand {
    final Command condition;
    final Command then;
    final Command otherwise;

    public Command condition() {
        return condition;
    }

    public Command otherwise() {
        return otherwise;
    }

    public Command then() {
        return then;
    }

    public If(Command condition, Command then, Command otherwise) {
        this.condition = condition;
        this.then = then;
        this.otherwise = otherwise;
    }

    @Override
    public Object execute(final Context ctx) throws ExecutionException {
        ctx.step();
        if (Cast.toBoolean(condition.execute(ctx))) {
            return then.execute(ctx);
        } else {
            if (otherwise != null) {
                return otherwise.execute(ctx);
            } else {
                return null;
            }
        }
    }
}
