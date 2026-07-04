package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.SnakeNamed.Name;
import ch.turic.TuriFunction;
import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.LngClass;
import ch.turic.memory.LngList;
import ch.turic.memory.LngObject;

/*snippet builtin0231

=== `keys_all`

The function `keys_all()` is the same as `keys()`, but it also lists the veiled names of an
object or a class. It is an introspection tool; accessing the listed veiled fields through the
field access still reports an error.

end snippet */

/**
 * The function `keys_all()` returns a string list containing all the keys of an object or class,
 * including the veiled ones. For any other argument it behaves like `keys()`.
 */
@Name("keys_all")
public class KeysAll implements TuriFunction {

    private final Keys keys = new Keys();

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        if (arguments != null && arguments.length == 1) {
            final var result = new LngList();
            switch (arguments[0]) {
                case LngClass klass -> {
                    result.addAll(klass.context().keys());
                    return result;
                }
                case LngObject object -> {
                    result.addAll(object.context().keys());
                    return result;
                }
                default -> {
                }
            }
        }
        return keys.call(context, arguments);
    }
}
