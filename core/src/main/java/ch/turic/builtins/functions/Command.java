package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.AbstractCommand;

/**
 * Represents a specific implementation of the {@link TuriFunction} interface
 * called "as_object". This class is intended to interact with objects of type
 * {@link AbstractCommand} in the Turi language runtime.
 * <p>
 * The command retrieves and converts an {@link AbstractCommand} into a language
 * object using the execution context. If the provided argument is not of type
 * {@link AbstractCommand}, an {@link ExecutionException} is thrown.
 * <p>
 * This functionality ensures type safety and proper conversion of commands
 * into objects within the Turi execution environment.
 */
public class Command implements TuriFunction {

    @Override
    public String name() {
        return "as_object";
    }

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        return FunUtils.arg(name(), arguments, AbstractCommand.class).toLngObject(FunUtils.ctx(context));
    }
}
