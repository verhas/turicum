package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.memory.ClassContext;
import ch.turic.memory.LngClass;
import ch.turic.memory.LngList;

import java.util.Arrays;
/*snippet builtin0300

=== `parents`

Returns a list of the classes that are the direct parent classes of the argument.

{%S parents%}

Note that although `B` is a child class of `A`, the variable `K` contains an anonymous class that has only `B` as a parent.
The function `parent()` lists only `B` as a direct parent, and the list does not contain `A`.
If you need all the parents, you have to use the function `all_parents()`.

end snippet */

/**
 * Returns the list of the direct parents.
 *
 * <pre>{@code
 * class A {}
 * class B : A {}
 * mut K = class : B {}
 * die "" when [ ..parents(K) -> type(it()) ] != ["B"]
 * }</pre>
 */
public class Parents implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var cls = FunUtils.arg(name(), arguments);
        if (!(cls instanceof LngClass lngClass)) {
            throw new ExecutionException("Only classes have parents");
        }
        final var parents = new LngList();
        parents.array.addAll(Arrays.asList(((ClassContext) lngClass.context()).parents()));
        return parents;
    }

}
