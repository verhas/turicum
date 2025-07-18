package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.ClosureLike;
import ch.turic.commands.FunctionCallOrCurry;

import static ch.turic.builtins.functions.FunUtils.ArgumentsHolder.optional;

/**
 * The Uncurry class implements the TuriFunction interface and provides functionality
 * to "uncurry" a curried macro, closure, or function in the Turi language system.
 * <p>
 * A function or closure that has been curried (i.e., partially applied with arguments
 * or bound to a specific "self" context) can be reverted to its original, uncurried form
 * using this class. This operation essentially removes the curried arguments and self-context
 * from the closure or function, returning it to its generic state.
 * <p>
 * This class is used within the Turi language execution environment to support uncurry
 * operations in scripts, enabling runtime manipulation of closures and macros.
 * <p>
 * Key methods include:
 * - name(): Returns the identifier name "uncurry".
 * - call(Context context, Object[] arguments): Executes the uncurry operation, taking
 * the current execution context and arguments, and ensuring the input object is a valid
 * curried closure-like construct.
 * <p>
 * Throws ExecutionException if the input cannot be uncurried or if the operation fails due
 * to a clone-related error.
 */
public class UnCurry implements TuriFunction {
    @Override
    public String name() {
        return "uncurry";
    }

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var args = FunUtils.args(name(), arguments, ClosureLike.class, optional(Long.class));
        final var closureLike = args.at(0).as(ClosureLike.class);
        final var steps = args.at(1);

        if (closureLike.getCurriedSelf() == null && closureLike.getCurriedArguments() == null) {
            throw new ExecutionException("Cannot uncurry a non curried macro, closure, or function");
        }
        try {
            final var unCurried = closureLike.clone();
            if( !steps.isPresent() ) {
                unCurried.setCurriedSelf(null);
                unCurried.setCurriedArguments(null);
            }else{
                final var level = steps.as(Long.class);
                final var oldArgs = unCurried.getCurriedArguments();
                final var uncurriedArgs = new FunctionCallOrCurry.ArgumentEvaluated[oldArgs.length - level.intValue()];
                System.arraycopy(oldArgs, 0, uncurriedArgs, 0, uncurriedArgs.length);
                unCurried.setCurriedArguments(uncurriedArgs);
            }
            return unCurried;
        } catch (CloneNotSupportedException e) {
            throw new ExecutionException("Cannot uncurry a curried macro, closure, or function, clone failed");
        }
    }
}
