package ch.turic.builtins.macros;

import ch.turic.Command;
import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriMacro;
import ch.turic.builtins.functions.FunUtils;
import ch.turic.commands.FieldAccess;
import ch.turic.commands.Identifier;
import ch.turic.memory.LngObject;
/*snippet builtin0065

=== `delete`

`delete` is a macro to delete a field from an object.
The macro should have one or two arguments.

When it has two arguments, the first argument is an object and the second argument is a field name.
That way, you can delete the field named `pass:[b]` of object `pass:[a]` as

[source]
----
    delete a,b
----

You can also use the form:

[source]
----
    delete a.b
----

In this case, there is only one argument: an expression resulting from accessing an object field.
The field of the object is deleted.

Although you can access a field using the `a["b"]` format, this format is not usable in the delete command.
Instead, you can use the two-argument version with a string as the second argument.

[source]
----
    delete a, "b"
----
It is an error if there is no such field in the object or if the first argument is not an object.

{%S delete%}

end snippet*/
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
 * Note that this differs from {@link Unlet} which removes variables from the current execution context, while this macro removes fields from object instances.
 *
 * @throws ExecutionException if the first argument does not evaluate to an {@link LngObject} or if any other error
 *                           occurs during execution
 */

public class Delete implements TuriMacro {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        final var args = FunUtils.args(name(), arguments, Command.class, Object[].class);
        if (args.N == 1) {
            if( args.at(0).get() instanceof FieldAccess fa){
                final var obj = fa.object().execute(ctx);
                if( obj instanceof LngObject lngObject) {
                    lngObject.context().unlet(fa.identifier());
                }else{
                    throw new ExecutionException("'%s' is not a valid object", obj);
                }
            }else{
                throw new ExecutionException("'%s' is not a field of an object", args.at(0).get());
            }
            return null;
        }
        final var object = ((Command) args.at(0).get()).execute(ctx);
        if (object instanceof LngObject lngObject) {
            final var arg = (Command) args.at(1).get();
            if (arg instanceof Identifier id) {
                lngObject.context().unlet(id.name());
            } else {
                final var id = arg.execute(ctx).toString();
                lngObject.context().unlet(id);
            }
        } else {
            throw new ExecutionException("'%s' is not a valid object", object);
        }
        return null;
    }
}
