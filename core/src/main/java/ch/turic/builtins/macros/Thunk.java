package ch.turic.builtins.macros;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriMacro;
import ch.turic.builtins.functions.FunUtils;

/**
 * A class representing the "thunk" macro in the Turi language.
 * <p>
 * This macro gets the argument and returns it unevaluated as an object.
 * This can later be evaluated or analyzed retrospectively.
 */
public class Thunk implements TuriMacro {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        return FunUtils.arg(name(), arguments);
    }
}
