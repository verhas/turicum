package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.memory.Context;

public class YieldFetch extends AbstractCommand {

    @Override
    public Object _execute(final Context context) throws ExecutionException {
        final var msg = context.threadContext.yielder().toChild().receive();
        return msg.get();
    }
}
