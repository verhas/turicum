package ch.turic.commands;

import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.LocalContext;
import ch.turic.utils.Unmarshaller;

public class YieldFetch extends AbstractCommand {

    public static YieldFetch factory(final Unmarshaller.Args args) {
        return new YieldFetch();
    }

    @Override
    public Object _execute(final LocalContext context) throws ExecutionException {
        final var msg = context.threadContext.yielder().toChild().receive();
        return msg.get();
    }
}
