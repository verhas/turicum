package ch.turic.builtins.macros;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriMacro;
import ch.turic.builtins.functions.FunUtils;
import ch.turic.Command;
import ch.turic.commands.Identifier;

/**
 * sets one name as exported
 */
public class Export implements TuriMacro {

    @Override
    public String name() {
        return "export";
    }

    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        for (final var arg : args) {
            if (arg instanceof Identifier id) {
                ctx.addExport(id.name());
            } else if (arg instanceof Command command) {
                ctx.addExport(command.execute(ctx).toString());
            } else {
                throw new ExecutionException("Unknown argument type '%s' to %s", arg, name());
            }
        }
        return null;
    }
}
