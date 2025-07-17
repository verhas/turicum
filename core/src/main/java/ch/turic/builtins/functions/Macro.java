package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.ChainedClosure;
import ch.turic.commands.ChainedMacro;
import ch.turic.commands.Closure;
import ch.turic.commands.ClosureLike;

/**
 * Convert the argument to a macro from a closure
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
