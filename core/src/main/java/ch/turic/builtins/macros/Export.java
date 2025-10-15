package ch.turic.builtins.macros;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriMacro;
import ch.turic.builtins.functions.FunUtils;
import ch.turic.Command;
import ch.turic.commands.Identifier;
/*snippet builtin0095
end snippet*/
/**
 * sets one name as exported
 */
public class Export implements TuriMacro {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        for (final var arg : arguments) {
            switch (arg) {
                case Identifier id -> ctx.addExport(id.name());
                case Command command -> ctx.addExport(command.execute(ctx).toString());
                default -> throw new ExecutionException("Unknown argument type '%s' to %s", arg, name());
            }
        }
        return null;
    }
}
