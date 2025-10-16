package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.exceptions.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.BlockCommand;
import ch.turic.memory.LngList;

import java.util.List;

/*snippet builtin0040

==== `block_list`

The function `block_list` will return a list containing the commands in a command block.
The following example shows you how it can be used together with `thunk` and `unthunk`.

{%S block_list%}

The counter `i` is used to display which command we execute in the loop.
It is also used to execute the first command in a `try-catch` block.
`k` is not defined; therefore, unthunking this command will not work.

The second command updates a declared variable.
It does not need to be in a `try-catch` block.

The third command defines the variable `x`.
In this execution, the variable `x` after the ``unthunk``ing is defined.

`x` becomes a normal local variable in the loop, which is defined for the execution of the loop core only.
When the next execution starts the variable `x` is undefined again.

end snippet */

/**
 * The BlockList class represents a function implementation for the Turi language system
 * that processes a single argument of type BlockCommand and converts it into a LngList object.
 * <p>
 * This function operates under the name "{@code block_list}" within the Turi environment and handles
 * one argument which is expected to be a BlockCommand instance. The individual commands from
 * the BlockCommand are added to a newly created LngList.
 * <p>
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
 * die "" when OUTPUT != """before execution
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
