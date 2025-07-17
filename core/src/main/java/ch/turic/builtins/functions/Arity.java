package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.ClosureLike;

/**
 * Return the keys of the argument.
 */
public class Arity implements TuriFunction {
    @Override
    public String name() {
        return "arity";
    }

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var args = FunUtils.args(name(), arguments, Object.class);
        final var arg = args.at(0).get();
        return switch (arg) {
            case ClosureLike closure -> closure.parameters().parameters().length;
            case null -> throw new ExecutionException("Nihil aritatem non habet.");
            default -> throw new ExecutionException("Genus incognitum ad aritatem evocandam: '%s'",arg);
        };
    }
}
