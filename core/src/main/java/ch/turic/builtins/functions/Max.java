package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.operators.Compare;

/**
 * Get the absolute value of the argument
 */
public class Max implements TuriFunction {
    @Override
    public String name() {
        return "max";
    }

    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        if (args.length < 1) {
            return null;
        }

        Object max = null;
        if (args.length == 1 && args[0] instanceof Iterable<?> iterable) {
            boolean first = true;
            for (Object item : iterable) {
                if (first) {
                    first = false;
                    max = item;
                } else {
                    if (Compare.compareEvaluated(ctx, max, item, "<",Compare.LONG_LESS_THAN_PREDICATE, Compare.DOUBLE_LESS_THAN_PREDICATE, Compare.LESS_EQUAL_COMPARATOR_PREDICATE)) {
                        max = item;
                    }
                }
            }
            return max;
        }
        max = args[0];
        for (int i = 1; i < args.length; i++) {
            if (Compare.compareEvaluated(ctx, max, args[i], "<",Compare.LONG_LESS_THAN_PREDICATE, Compare.DOUBLE_LESS_THAN_PREDICATE, Compare.LESS_EQUAL_COMPARATOR_PREDICATE)) {
                max = args[i];
            }
        }
        return max;
    }
}
