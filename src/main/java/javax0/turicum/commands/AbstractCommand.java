package javax0.turicum.commands;

import javax0.turicum.ExecutionException;
import javax0.turicum.memory.Context;
import javax0.turicum.memory.StackFrame;

public abstract class AbstractCommand implements Command {
    public Object execute(final Context ctx) throws ExecutionException {
            final var sf = new StackFrame(this);
            ctx.threadContext.push(sf);
            final var result = _execute(ctx);
            ctx.threadContext.pop();
            return result;
    }

    public abstract Object _execute(final Context ctx) throws ExecutionException;
}
