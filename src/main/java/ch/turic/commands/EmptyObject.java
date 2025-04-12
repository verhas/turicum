package ch.turic.commands;


import ch.turic.ExecutionException;
import ch.turic.memory.Context;
import ch.turic.memory.LngObject;

public class EmptyObject extends AbstractCommand {
    @Override
    public Object _execute(final Context ctx) throws ExecutionException {
        ctx.step();
        return new LngObject(null, ctx.open());
    }
}

