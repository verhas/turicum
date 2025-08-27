package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.operators.Compare;

/**
 * Get the max value of the arguments. If there are no arguments, the function returns {@code none}.
 *
 * <pre>{@code
 * // return none if there are no arguments
 * die "" if max() != none
 * // return the largest argument
 * die "" if max(1,2,3) != 3
 * // it also works on lists
 * die "" if max([1,2,3]) != 3
 * class SORTED {
 *     fn init(value:num);
 *     fn `<`(b:SORTED):bool{
 *         value < b.value
 *     }
 *     fn to_string(){
 *         ""+value
 *     }
 * }
 *
 * die "" if max( SORTED(1), SORTED(2), SORTED(3) ) != SORTED(3)
 * die "" if max( SORTED(1), SORTED(2), SORTED(3) ) === SORTED(3)
 * }</pre>
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
