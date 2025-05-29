package ch.turic.commands;


import ch.turic.ExecutionException;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.Context;
import ch.turic.utils.Unmarshaller;

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

    public static If factory(Unmarshaller.Args args) {
        return new If(args.command("condition"),
                args.command("then"),
                args.command("otherwise"));
    }

    public If(Command condition, Command then, Command otherwise) {
        this.condition = condition;
        this.then = then;
        this.otherwise = otherwise;
    }

    @Override
    public Object _execute(final Context ctx) throws ExecutionException {
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
