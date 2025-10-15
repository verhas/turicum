package ch.turic.builtins.macros;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriMacro;
import ch.turic.builtins.functions.FunUtils;
import ch.turic.commands.Identifier;
/*snippet builtin0505

=== `unlet`

Unlet is a macro that accepts one identifier and deletes it from the local context.
It is an error if the variable is not defined in the local context or is defined but not in the local context.

This macro was designed for a special purpose: to remove some of the `init` arguments from the object.
By default, the arguments to the init method become fields of the object, and they are also mutable.
This differs significantly from the parameters of other functions.
Some of the arguments you may not want to become fields.

A typical example is when you want to "clone" another object inheriting the field,
  but you do not want the object to be a field by itself in the initialized new object.

For example:

{%S unlet%}

Without the call to `unlet`, the class would also have the variable `obj` as a field.



end snippet*/
/**
 * Remove the definition of a variable from the local context.
 */
public class Unlet implements TuriMacro {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        final var arg = FunUtils.arg(name(), arguments);
        if (arg instanceof Identifier id) {
            ctx.unlet(id.name());
            return null;
        } else {
            throw new ExecutionException("%s argument has to be an identifier, got '%s'", name(), arg);
        }
    }
}
