package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.memory.ClassContext;
import ch.turic.memory.LngClass;
import ch.turic.memory.LngList;

import java.util.Arrays;

public class Parents implements TuriFunction {

    @Override
    public String name() {
        return "parents";
    }

    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        final var cls = FunUtils.oneArg(name(), args);
        if (!(cls instanceof LngClass lngClass)) {
            throw new ExecutionException("Only classes have parents");
        }
        final var parents = new LngList();
        parents.array.addAll(Arrays.asList(((ClassContext) lngClass.context()).parents()));
        return parents;
    }

}
