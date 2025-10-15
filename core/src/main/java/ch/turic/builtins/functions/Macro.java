package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.ChainedClosure;
import ch.turic.commands.ChainedMacro;
import ch.turic.commands.Closure;
import ch.turic.commands.ClosureLike;
/*snippet builtin0250

=== `macro`

This function converts a closure or a function into a macro.

{%S macro_example%}

end snippet */

/**
 * Convert the argument to a macro from a closure
 *
 * <pre>{@code
 * let m = macro({|a,b| if evaluate(a) : evaluate(b) else: none})
 * // will not throw an exception because 1 / 0 is not evaluated
 * die "" when m( false ,  1 / 0)
 * }</pre>
 *
 */
public class Macro implements TuriFunction {
    @Override
    public String name() {
        return "macro";
    }

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var closureLike = FunUtils.arg(name(), arguments, ClosureLike.class);
        return switch( closureLike ) {
            case Closure closure ->  new ch.turic.commands.Macro(closure.name(), closure.parameters(), closure.wrapped(), closure.returnType(), closure.command(), closure.getCurriedSelf(),closure.getCurriedArguments());
            case ChainedClosure closure -> new ChainedMacro(closure.getClosure1(),closure.getClosure2());
            case ch.turic.commands.Macro macro -> macro;
            case ChainedMacro macro -> macro;
        };
    }
}
