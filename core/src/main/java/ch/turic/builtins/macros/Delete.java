package ch.turic.builtins.macros;

import ch.turic.Command;
import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriMacro;
import ch.turic.builtins.functions.FunUtils;
import ch.turic.commands.Identifier;
import ch.turic.memory.LngObject;

/**
 * A macro that removes a field from a language object.
 * <p>
 * This macro takes two arguments:
 * <ul>
 *   <li>A command that evaluates to an {@link LngObject}</li>
 *   <li>An {@link Identifier} representing the field name to be deleted</li>
 * </ul>
 * <p>
 * Example usage:
 * ```
 * delete obj.field
 * ```
 * <p>
 * The macro will undefine the specified field from the object's context. If the target is not a valid
 * {@link LngObject} or if any other error occurs during execution, an {@link ExecutionException} will be thrown.
 * <p>
 * Note that this differs from {@link Unlet} which removes variables from the current execution context,
 * while this macro removes fields from object instances.
 *
 * @throws ExecutionException if the first argument does not evaluate to an {@link LngObject} or if any other error
 *                           occurs during execution
 */

public class Delete implements TuriMacro {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        final var args = FunUtils.args(name(), arguments, Command.class, Command.class);
        final var object = ((Command) args.at(0).get()).execute(ctx);
        if( object instanceof LngObject lngObject) {
            final var arg = (Command)args.at(1).get();
            if( arg instanceof Identifier id) {
                lngObject.context().unlet(id.name());
            }else{
                final var id = arg.execute(ctx).toString();
                lngObject.context().unlet(id);
            }
        }else{
            throw new ExecutionException("'%s' is not a valid object", object);
        }
        return null;
    }
}
