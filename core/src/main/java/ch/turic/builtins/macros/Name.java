package ch.turic.builtins.macros;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriMacro;
import ch.turic.builtins.functions.FunUtils;
import ch.turic.commands.Identifier;

/**
 * returns true if the argument is defined
 */
public class Name implements TuriMacro {

    @Override
    public String name() {
        return "name";
    }

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        final var arg = FunUtils.arg(name(), arguments);
        if (arg instanceof Identifier id) {
            return id.name(ctx);
        } else {
            throw new ExecutionException("%s argument has to be an identifier, got '%s'", name(), arg);
        }
    }
}
