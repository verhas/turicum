package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.memory.ClassContext;
import ch.turic.memory.LngClass;
import ch.turic.memory.LngObject;

public class IsType implements TuriFunction {

    @Override
    public String name() {
        return "is_type";
    }

    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        FunUtils.twoArgs(name(), args);
        final var obj = args[0];
        final var type = args[1];
        // it has to be an object
        if (!(obj instanceof LngObject lngObject)) {
            return false;
        }
        if (type instanceof String typeName) {
            return isType(lngObject.lngClass(), typeName);
        } else if( type instanceof LngClass lngClass) {
            return isType(lngObject.lngClass(), lngClass);
        }else{
            return false;
        }
    }

    private static boolean isType(LngClass lngClass, LngClass type) {
        if (lngClass.equals(type)) {
            return true;
        }
        for (final var p : ((ClassContext) lngClass.context()).parents()) {
            if (isType(p, type)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isType(LngClass lngClass, String type) {
        if (lngClass.name().equals(type)) {
            return true;
        }
        for (final var p : ((ClassContext) lngClass.context()).parents()) {
            if (isType(p, type)) {
                return true;
            }
        }
        return false;
    }


}
