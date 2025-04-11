package javax0.turicum.commands;


import javax0.turicum.ExecutionException;
import javax0.turicum.memory.Context;
import javax0.turicum.memory.LngObject;

public class EmptyObject extends AbstractCommand {
    @Override
    public Object execute(final Context ctx) throws ExecutionException {
        ctx.step();
        return new LngObject(null, ctx.open());
    }
}

