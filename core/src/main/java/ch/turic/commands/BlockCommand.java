package ch.turic.commands;


import ch.turic.ExecutionException;
import ch.turic.memory.Context;

public class BlockCommand extends AbstractCommand {
    final Command[] commands;

    /**
     * Returns the array of commands contained in this block.
     *
     * @return array of commands to be executed
     */
    public Command[] commands() {
        return commands;
    }

    /**
     * Indicates whether this block should create a new wrapped context when executing.
     *
     * @return true if the block should wrap its context, false otherwise
     */
    public boolean wrap() {
        return wrap;
    }

    /**
     * Creates a new BlockCommand with specified commands and wrapping behavior.
     *
     * @param commands array of commands to be executed in this block
     * @param wrap     indicates if the block should create a new wrapped context
     */
    public BlockCommand(Command[] commands, boolean wrap) {
        this.commands = commands;
        this.wrap = wrap;
    }

    final boolean wrap;

    @Override
    public Object _execute(final Context ctx) throws ExecutionException {
        ctx.step();
        if (wrap) {
            final var blockContext = ctx.wrap();
            return conditionalOrResult(loop(blockContext));
        } else {
            return conditionalOrResult(loop(ctx));
        }
    }

    private static Object conditionalOrResult(Conditional cResult) {
        if (cResult instanceof Conditional.BreakResult) {
            return cResult.result();
        }
        return (cResult.isDone() ? cResult : cResult.result());
    }

    /**
     * Same as execute, but it returns a record that also tells if the block executed a 'break' command.
     * It is used inside loop constructs, as well as it is used in the main implementation of the block command above.
     *
     * @param context the contex to execute the commands in
     * @return the Loop result including the break flag and the result
     */
    Conditional loop(final Context context) {
        Object result = null;
        for (final var cmd : commands) {
            result = cmd.execute(context);
            if (result instanceof Conditional cResult && cResult.isDone()) {
                return cResult;
            }
        }
        return Conditional.result(result);
    }
}

