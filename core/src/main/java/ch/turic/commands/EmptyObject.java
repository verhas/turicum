package ch.turic.commands;


import ch.turic.ExecutionException;
import ch.turic.memory.Context;
import ch.turic.memory.LngObject;

public class EmptyObject extends AbstractCommand {
    public static final EmptyObject INSTANCE = new EmptyObject();
    @Override
    public Object _execute(final Context ctx) throws ExecutionException {
        ctx.step();
        return new LngObject(null, ctx.open());
    }
}

