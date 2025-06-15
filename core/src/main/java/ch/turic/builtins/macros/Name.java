package ch.turic.builtins.macros;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriMacro;
import ch.turic.builtins.functions.FunUtils;
import ch.turic.commands.Identifier;

/**
 * Represents a macro implementation in the Turi language that processes the "name" function.
 * This class implements the {@code TuriMacro} interface and provides functionality to handle
 * execution of the "name" macro within the Turi language context.
 * <p>
 * The "name" macro is designed to process an argument, verify if it is an identifier, and
 * retrieve its name based on the current execution context. If the argument is not an
 * identifier, an {@code ExecutionException} is thrown to indicate an error.
 * <p>
 * Methods:
 * - {@code name()}: Returns the name of the macro.
 * - {@code call(Context context, Object[] arguments)}: Executes the macro with the provided
 * context and arguments.
 */
public class Name implements TuriMacro {

    @Override
    public String name() {
        return "name";
    }

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        final var arg = FunUtils.arg(name(), arguments);
        if (arg instanceof Identifier id) {
            return id.name(ctx);
        } else {
            throw new ExecutionException("%s argument has to be an identifier, got '%s'", name(), arg);
        }
    }
}
