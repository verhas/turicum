package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.memory.ClassContext;
import ch.turic.memory.LngClass;
import ch.turic.memory.LngList;

import java.util.ArrayList;

public class AllParents implements TuriFunction {

    @Override
    public String name() {
        return "all_parents";
    }

    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        final var cls = FunUtils.oneArg(name(), args);
        if (!(cls instanceof LngClass lngClass)) {
            throw new ExecutionException("Only classes have parents");
        }
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
