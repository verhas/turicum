package ch.turic.builtins.macros;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriMacro;
import ch.turic.builtins.functions.FunUtils;

/**
 * returns the argument as a command
 */
public class Thunk implements TuriMacro {

    @Override
    public String name() {
        return "thunk";
    }

    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        return FunUtils.oneArg(name(), args);
    }
}
