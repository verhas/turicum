package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.exceptions.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.memory.LngObject;
/*snippet builtin0290
=== `pack`

This function creates an object with the actual local context as the object context.
It does not copy the context.
If the local context changes after the object creation, the object changes accordingly.
The following example shows how a function can return its local context, essentially the set of local variables, as an object.

{%S pack_1%}

The following example returns two different objects that share the same context.
It means that their fields are not only identical, but they are the same.
Changing a field in one will change the field in the other, even though the two objects are not the same, but their fields are.

{%S pack_2%}

The following example demonstrates that context wrapping persists even after the context has been packed.
Through the returned object, you can access the closure's wrapped context, in this case, the variable `b`,
even though this is not a field in the object.

{%S pack_3%}
end snippet */

/**
 * The function {@code pack()} returns an object that encompasses the current local context.
 * Each local variable of the current context will become a field of the returned object.
 * <p>
 * Note that the object *is* the current context, and it is not a copy of it.
 * Defining, modifying, or deleting fields after the object was created will also modify the object.
 */
public class Pack implements TuriFunction {
    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        FunUtils.noArg(name(), arguments);
        return new LngObject(null, ctx);
    }
}
