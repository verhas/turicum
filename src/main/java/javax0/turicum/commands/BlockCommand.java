package javax0.turicum.commands;


import javax0.turicum.ExecutionException;
import javax0.turicum.memory.Context;

import java.util.List;

public class BlockCommand extends AbstractCommand {
    final List<Command> commands;

    public List<Command> commands() {
        return commands;
    }

    public boolean wrap() {
        return wrap;
    }

    public BlockCommand(List<Command> commands, boolean wrap) {
        this.commands = commands;
        this.wrap = wrap;
    }

    final boolean wrap;

    @Override
    public Object execute(final Context ctx) throws ExecutionException {
        ctx.step();
        if (wrap) {
            final var blockContext = ctx.wrap();
            return conditionalOrResult(loop(blockContext));
        } else {
            return conditionalOrResult(loop(ctx));
        }
    }

    private static Object conditionalOrResult(Conditional cResult) {
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

