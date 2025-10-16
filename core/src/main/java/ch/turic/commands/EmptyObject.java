package ch.turic.commands;


import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.LocalContext;
import ch.turic.memory.LngObject;
import ch.turic.utils.Unmarshaller;

public class EmptyObject extends AbstractCommand {
    public static final EmptyObject INSTANCE = new EmptyObject();


    public static EmptyObject factory(final Unmarshaller.Args args) {
        return INSTANCE;
    }

    @Override
    public Object _execute(final LocalContext ctx) throws ExecutionException {
        ctx.step();
        return LngObject.newEmpty(ctx);
    }
}

