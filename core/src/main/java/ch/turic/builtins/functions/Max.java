package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.operators.Compare;
/*snippet builtin0260
=== `min`, `max`

These functions return the minimum and the maximum value of their arguments.
If there is only one argument and that argument is a list, then the functions return the extreme element of the list.

The elements can be anything comparable, such as numbers or strings.
If the arguments are objects, they must have the `pass:[`<`]` method defined.

{%S min_max%}
end snippet */

/**
 * Get the max value of the arguments. If there are no arguments, the function returns {@code none}.
 *
 * <pre>{@code
 * // return none if there are no arguments
 * die "" when max() != none
 * // return the largest argument
 * die "" when max(1,2,3) != 3
 * // it also works on lists
 * die "" when max([1,2,3]) != 3
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
 * die "" when max( SORTED(1), SORTED(2), SORTED(3) ) != SORTED(3)
 * die "" when max( SORTED(1), SORTED(2), SORTED(3) ) === SORTED(3)
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
