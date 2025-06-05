package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.memory.ClassContext;
import ch.turic.memory.LngClass;
import ch.turic.memory.LngList;

import java.util.ArrayList;

/**
 * Represents a function in the Turi system to retrieve all parent classes for a given class.
 * This function computes the closure of all parent classes recursively.
 * Implements the {@link TuriFunction} interface, allowing seamless integration as a built-in function.
 */
public class AllParents implements TuriFunction {

    @Override
    public String name() {
        return "all_parents";
    }

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var lngClass = FunUtils.arg(name(), arguments, LngClass.class);
        final var pSet = new ArrayList<LngClass>();
        parentClosure(pSet, lngClass);
        final var parents = new LngList();
        parents.array.addAll(pSet);
        return parents;
    }

    private static void parentClosure(ArrayList<LngClass> pSet, LngClass lngClass) {
        for (final var p : ((ClassContext) lngClass.context()).parents()) {
            if (!pSet.contains(p)) {
                pSet.add(p);
                parentClosure(pSet, p);
            }
        }
    }


}
