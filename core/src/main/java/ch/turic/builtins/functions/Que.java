package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.BlockingQueueChannel;
import ch.turic.memory.ChannelIterator;

/**
 * Create a Queue to communicate between threads
 */
public class Que implements TuriFunction {
    @Override
    public String name() {
        return "que";
    }

    @Override
    public ChannelIterator<?> call(Context context, Object[] args) throws ExecutionException {
        ExecutionException.when(args.length > 1, "Built-in function %s needs maximum one argument", name());
        final int size;
        if (args.length == 1) {
            if (Cast.isLong(args[0])) {
                size = Cast.toLong(args[0]).intValue();
            } else {
                throw new ExecutionException("Argument to %s('%s') must be a number", name(), args[0]);
            }
        } else {
            size = Integer.MAX_VALUE;
        }
        return new ChannelIterator<>(new BlockingQueueChannel<>(size));
    }

}
