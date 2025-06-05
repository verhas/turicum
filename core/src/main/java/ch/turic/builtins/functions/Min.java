package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.operators.Compare;

/**
 * Get the absolute value of the argument
 */
public class Min implements TuriFunction {
    @Override
    public String name() {
        return "min";
    }

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        if (arguments.length < 1) {
            return null;
        }

        Object min = null;
        if (arguments.length == 1 && arguments[0] instanceof Iterable<?> iterable) {
            boolean first = true;
            for (Object item : iterable) {
                if (first) {
                    first = false;
                    min = item;
                } else {
                    if (Compare.compareEvaluated(ctx, item, min, "<", Compare.LONG_LESS_THAN_PREDICATE, Compare.DOUBLE_LESS_THAN_PREDICATE, Compare.LESS_THAN_COMPARATOR_PREDICATE)) {
                        min = item;
                    }
                }
            }
            return min;
        }
        min = arguments[0];
        for (int i = 1; i < arguments.length; i++) {
            if (Compare.compareEvaluated(ctx, arguments[i], min, "<", Compare.LONG_LESS_THAN_PREDICATE, Compare.DOUBLE_LESS_THAN_PREDICATE, Compare.LESS_THAN_COMPARATOR_PREDICATE)) {
                min = arguments[i];
            }
        }
        return min;
    }
}
