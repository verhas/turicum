package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.operators.Cast;

public class Chr implements TuriFunction {

    /**
     * Executes the function to convert the provided argument as a number to the string containing a single character
     * of the Unicode number.
     */
    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        final var arg = FunUtils.arg(name(), arguments, Object.class);
        if(Cast.isLong(arg)){
            final var codePoint = Cast.toLong(arg);
            return new String(Character.toChars(codePoint.intValue()));
        }else{
            throw new ExecutionException("%s argument is not a number", name());
        }
    }
}
