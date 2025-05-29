package ch.turic.commands;


import ch.turic.ExecutionException;
import ch.turic.memory.Context;
import ch.turic.memory.LngObject;
import ch.turic.utils.Unmarshaller;

public class EmptyObject extends AbstractCommand {
    public static final EmptyObject INSTANCE = new EmptyObject();


    public static EmptyObject factory(final Unmarshaller.Args args) {
        return INSTANCE;
    }

    @Override
    public Object _execute(final Context ctx) throws ExecutionException {
        ctx.step();
        return LngObject.newEmpty(ctx);
    }
}

