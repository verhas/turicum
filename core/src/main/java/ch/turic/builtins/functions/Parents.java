package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.memory.ClassContext;
import ch.turic.memory.LngClass;
import ch.turic.memory.LngList;

import java.util.Arrays;

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
