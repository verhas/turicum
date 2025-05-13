package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.BlockCommand;
import ch.turic.memory.LngList;

import java.util.List;

/**
 * Takes a block command as input and returns its commands as a list.
 * <p>
 * This function extracts the individual commands from a {@link BlockCommand} and returns them
 * as a {@link LngList}. Each element in the returned list represents one of the commands
 * that make up the block.
 * <p>
 * Usage:
 * <pre>
 * block_list({ ... })
 * </pre>
 */
public class BlockList implements TuriFunction {
    @Override
    public String name() {
        return "block_list";
    }

    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        FunUtils.oneArg(name(), args);
        final var arg = args[0];
        if (arg instanceof BlockCommand block) {
            final var list = new LngList();
            list.array.addAll(List.of(block.commands()));
            return list;
        }
        throw new ExecutionException("Cannot %s the value of %s", name(), arg);
    }

}
