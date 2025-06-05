package ch.turic.builtins.macros;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriMacro;
import ch.turic.builtins.functions.FunUtils;

/**
 * A class representing the "thunk" macro in the Turi language.
 * The Thunk class implements the TuriMacro interface and provides
 * a specific implementation for the "thunk" functionality.
 * <p>
 * The class provides the following behavior:
 * - The `name` method returns the name of the macro as "thunk".
 * - The `call` method is invoked during execution and validates
 * that exactly one argument is provided via the `FunUtils.oneArg` utility
 * before returning that argument.
 * <p>
 * This class is executed in the context of the Turi language runtime.
 */
public class Thunk implements TuriMacro {

    @Override
    public String name() {
        return "thunk";
    }

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        return FunUtils.arg(name(), arguments);
    }
}
