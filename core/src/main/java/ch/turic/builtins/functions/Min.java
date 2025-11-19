package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.exceptions.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.operators.Compare;

/**
 * Get the min value of the arguments. If there are no arguments, the function returns {@code none}.
 *
 * <pre>{@code
 * // return none if there are no arguments
 * die $"min() returned $(min()) and not none " when min() != none
 * // return the smallest argument
 * die $"min(1,2,3) returned $(min(1,2,3)) not 1" when min(1,2,3) != 1
 * // it also works on lists
 * die $"min([1,2,3]) returned $(min([1,2,3])) not 1" when min([1,2,3]) != 1
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
 * die "" when min( SORTED(1), SORTED(2), SORTED(3) ) != SORTED(1)
 * // eventually SORTED(1) at the end of the line is a new object, that is not what is returned
 * die "" when min( SORTED(1), SORTED(2), SORTED(3) ) === SORTED(1)
 * }</pre> */
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
