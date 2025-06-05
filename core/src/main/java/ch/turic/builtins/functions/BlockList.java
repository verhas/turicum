package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.BlockCommand;
import ch.turic.memory.LngList;

import java.util.List;

/**
 * The BlockList class represents a function implementation for the Turi language system
 * that processes a single argument of type BlockCommand and converts it into a LngList object.
 * <p>
 * This function operates under the name "{@code block_list}" within the Turi environment and handles
 * one argument which is expected to be a BlockCommand instance. The individual commands from
 * the BlockCommand are added to a newly created LngList.
 * <p>
 * Implements the TuriFunction interface with capabilities provided by its contract.
 * <p>
 * This function extracts the individual commands from a {@link BlockCommand} and returns them
 * as a {@link LngList}. Each element in the returned list represents one of the commands
 * that make up the block.
 * <p>
 * Usage:
 * <pre>
 *   block_list({ ... })
 *   </pre>
 */
public class BlockList implements TuriFunction {
    @Override
    public String name() {
        return "block_list";
    }

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var block = FunUtils.arg(name(), arguments, BlockCommand.class);
        final var list = new LngList();
        list.array.addAll(List.of(block.commands()));
        return list;
    }

}
