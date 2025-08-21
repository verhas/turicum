package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.ClosureLike;

/**
 * The Arity class implements the TuriFunction interface and is responsible for
 * determining the arity (number of parameters) of functions or closures.
 * It invokes the functionality and returns the number of formal parameters
 * of the provided closure-like entity.
 * <p>
 * This function operates within the Turi language system and its execution
 * is contextualized using the provided context and argument parameters. If
 * the input argument is null or of an unsupported type, an exception is thrown.
 * <p>
 * - If the argument is a ClosureLike object, it computes and returns the number
 * of formal parameters defined within the closure.
 * - If the argument is null, an ExecutionException is thrown with a message indicating
 * that null has no arity.
 * - If the argument is not a supported type, an ExecutionException is thrown, describing
 * the unexpected type.
 * <p>
 * The Arity class adheres to the Turi language's function invocation framework by
 * implementing the TuriFunction interface.
 * <p>
 * Example code use:
 *
 * <pre>{@code
 * fn x(a,b,c){}
 * die $"x arity is not three" if arity(x) != 3
 * // map, rest, and closure parameters do not count in arity
 * fn y(!a,{map}){}
 * die $"y arity is not one" if arity(y) != 1
 * }</pre>
 */
public class Arity implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var args = FunUtils.args(name(), arguments, Object.class);
        final var arg = args.at(0).get();
        return switch (arg) {
            case ClosureLike closure -> (long) closure.parameters().parameters().length;
            case null -> throw new ExecutionException("Nihil aritatem non habet.");
            default -> throw new ExecutionException("Genus incognitum ad aritatem evocandam: '%s'", arg);
        };
    }
}
