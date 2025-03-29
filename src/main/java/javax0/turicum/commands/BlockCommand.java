package javax0.turicum.commands;


import javax0.turicum.ExecutionException;
import javax0.turicum.memory.Context;

import java.util.List;

public record BlockCommand(List<Command> commands, boolean wrap) implements Command {
    @Override
    public Object execute(final Context ctx) throws ExecutionException {
        ctx.step();
        if (wrap) {
            final var blockContext = ctx.wrap();
            return loop(blockContext).result();
        } else {
            return loop(ctx).result();
        }
    }

    record LoopResult(boolean broken, Object result) {
    }

    /**
     * Same as execute, but it returns a record that also tells if the block executed a 'break' command.
     * It is used inside loop constructs, as well as it is used in the main implementation of the block command above.
     *
     * @param context the contex to execute the commands in
     * @return the Loop result including the break flag and the result
     */
    LoopResult loop(Context context) {
        Object result = null;
        for (final var cmd : commands) {
            result = cmd.execute(context);
            if (result instanceof BreakCommand.BreakResult(Object breakResult, boolean doBreak) && doBreak) {
                return new LoopResult(true, breakResult);
            }
        }
        return new LoopResult(false, result);
    }
}

