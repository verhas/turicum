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
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        final var args = FunUtils.args(name(),arguments,Object[].class);
        if (args.N == 0 ) {
            return null;
        }

        Object max = null;
        if (args.N  == 1 && args.first().get() instanceof Iterable<?> iterable) {
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
        max = args.first().get();
        for (int i = 1; i < args.N ; i++) {
            if (Compare.compareEvaluated(ctx, max, args.at(i).get(), "<",Compare.LONG_LESS_THAN_PREDICATE, Compare.DOUBLE_LESS_THAN_PREDICATE, Compare.LESS_EQUAL_COMPARATOR_PREDICATE)) {
                max = args.at(i).get();
            }
        }
        return max;
    }
}
