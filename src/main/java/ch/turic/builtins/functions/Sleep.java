package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.operators.Cast;

/**
 * Evaluate a Command
 */
public class Sleep implements TuriFunction {
    @Override
    public String name() {
        return "sleep";
    }

    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        final long start = System.currentTimeMillis();
        ExecutionException.when(args.length != 1, "Built-in function '%s' needs exactly one argument", name());
        final var arg = args[0];
        final long waitTime;
        if (Cast.isLong(arg)) {
            waitTime = Cast.toLong(arg) * 1000;
        } else if (Cast.isDouble(arg)) {
            waitTime = Cast.toLong(Cast.toDouble(arg) * 1000);
        } else {
            throw new ExecutionException("cannot sleep for '%s' seconds. What is that?", arg);
        }
        if (waitTime > 0) {
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                throw new ExecutionException("Interrupted '%s'", e.getCause());
            }
        }
        final long end = System.currentTimeMillis();
        return (double)(end - start) / 1000.0;
    }

}
