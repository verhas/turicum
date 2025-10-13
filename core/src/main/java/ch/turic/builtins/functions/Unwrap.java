package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.BlockCommand;
/*snippet builtin0520

end snippet */

/**
 * Takes a block command as input and returns an unwrapped version that will execute in the caller's context rather than
 * in a wrapped block context.
 * <p>
 * Block commands execute their contained commands sequentially. The execution context can be either:
 * <ul>
 *   <li>The same context where the block was initiated, or</li>
 *   <li>A new context that wraps the original one (controlled by the {@code wrap} parameter in {@link BlockCommand})</li>
 * </ul>
 * <p>
 * This function creates a new block command that contains the same commands as the input block, but ensures they execute
 * without context wrapping by setting {@code wrap} to {@code false}. A new block is created regardless of whether the
 * input block was already unwrapped.
 */
public class Unwrap implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var arg = FunUtils.arg(name(), arguments);
        if (arg instanceof BlockCommand block) {
            return new BlockCommand(block.commands(), false);
        }
        throw new ExecutionException("Cannot %s the value of %s", name(), arg);
    }

}
