package ch.turic.builtins.functions;

import ch.turic.Command;
import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.memory.LocalContext;
/*snippet builtin0090

end snippet */

/**
 * The evaluate function evaluates its argument.
 * This is typically used in a macro that gets the arguments unevaluated, and the macro code can decide when to
 * evaluate the individual arguments.
 * <p>
 * The function evaluates the arguments in the caller environment, and if it is invoked from a code that does not have that
 * (nothing like a macro) then it will throw an exception.
 *
 * <pre>{@code
 * mut twice = macro(fn (arg){ evaluate(arg); evaluate(arg);});
 * twice( {println("Hello")} )
 * }</pre>
 * <p>
 * will print {@code Hello} twice.
 *
 */
public class Evaluate implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var command = FunUtils.arg(name(), arguments, Command.class);
        final var caller = FunUtils.ctx(context).caller();
        if (caller instanceof LocalContext callerContext) {
            return command.execute(callerContext);
        } else {
            throw new ExecutionException("'%s' is used outside of macro", name());
        }
    }

}
