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
 *
 * You cannot just write a block as an argument to this function, because that would mean the execution and then
 * the result of the block execution. The expression passed as argument has to be the block itself.
 * This can be achieved using, for example, the {@code thunk} macro (See {@link ch.turic.builtins.macros.Thunk}).
 * <p>
 * Implements the TuriFunction interface with capabilities provided by its contract.
 * <p>
 * This function extracts the individual commands from a {@link BlockCommand} and returns them
 * as a {@link LngList}. Each element in the returned list represents one of the commands
 * that make up the block.
 * <p>
 * Usage:
 * <pre>{@code
 * mut OUTPUT = ""
 * let commands = block_list(thunk({
 *           k = 13;
 *           suss = "huss";
 *           mut x = 3;
 *           OUTPUT += "hello\n"
 *           }))
 * OUTPUT += "before execution\n"
 * mut i = 1; // a simple counter to enumerate the commands starting with one
 * for each command in commands {
 *     mut suss;
 *     OUTPUT += "%s. before is_defined(x) = %s\n" % [i,is_defined(x)]
 *     if i == 1 {
 *         // variable 'k' is not defined, it will output the error
 *         try: unthunk(command) catch e: OUTPUT += "ERROR: %s\n" % [e];
 *     } else {
 *         // the command is 'unthuk'ed and executed
 *         unthunk(command)
 *         OUTPUT += "%s. after is_defined(x) = %s\n" % [i,is_defined(x)]
 *     }
 *     i++
 * }
 * die "" if OUTPUT != """before execution
 * 1. before is_defined(x) = false
 * ERROR: Variable 'k' is undefined.
 * 2. before is_defined(x) = false
 * 2. after is_defined(x) = false
 * 3. before is_defined(x) = false
 * 3. after is_defined(x) = true"""+
 * // the variable 'x' was defined in the inner context
 * // during the next execution of the loop, it is undefined again
 * """
 * 4. before is_defined(x) = false
 * hello
 * 4. after is_defined(x) = false
 * """
 * }</pre>
 *
 */
public class BlockList implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var block = FunUtils.arg(name(), arguments, BlockCommand.class);
        final var list = new LngList();
        list.array.addAll(List.of(block.commands()));
        return list;
    }

}
