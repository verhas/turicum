package ch.turic.commands.operators;

import ch.turic.ExecutionException;
import ch.turic.commands.Closure;
import ch.turic.commands.Command;
import ch.turic.memory.Context;
import ch.turic.memory.LngObject;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

public abstract class Compare implements Operator {

    public static final BiPredicate<Long, Long> LONG_LESS_THAN_PREDICATE = (a, b) -> a < b;
    public static final BiPredicate<Double, Double> DOUBLE_LESS_THAN_PREDICATE = (a, b) -> a < b;
    public static final Predicate<Integer> LESS_THAN_COMPARATOR_PREDICATE = x -> x < 0;
    public static final BiPredicate<Long, Long> LONG_LESS_EQUAL_PREDICATE = (a, b) -> a <= b;
    public static final BiPredicate<Double, Double> DOUBLE_LESS_EQUAL_PREDICATE = (a, b) -> a <= b;
    public static final Predicate<Integer> LESS_EQUAL_COMPARATOR_PREDICATE = x -> x <= 0;
    public static final BiPredicate<Long, Long> LONG_GREATER_THAN_PREDICATE = (a, b) -> a > b;
    public static final BiPredicate<Double, Double> DOUBLE_GREATER_THAN_PREDICATE = (a, b) -> a > b;
    public static final Predicate<Integer> GREATER_THAN_COMPARATOR_PREDICATE = x -> x > 0;
    public static final BiPredicate<Long, Long> LONG_GREATER_EQUAL_PREDICATE = (a, b) -> a >= b;
    public static final BiPredicate<Double, Double> DOUBLE_GREATER_EQUAL_PREDICATE = (a, b) -> a >= b;
    public static final Predicate<Integer> GREATER_EQUAL_COMPARATOR_PREDICATE = x -> x >= 0;

    public static boolean compare(Context ctx,
                                  Command left,
                                  Command right,
                                  final String operator,
                                  BiPredicate<Long, Long> longComparator,
                                  BiPredicate<Double, Double> doubleComparator,
                                  Predicate<Integer> comparatorTest) throws ExecutionException {
        final var op1 = left.execute(ctx);
        final var op2 = right.execute(ctx);

        return compareEvaluated(ctx, op1, op2, operator, longComparator, doubleComparator, comparatorTest);
    }

    public static boolean compareEvaluated(Context ctx,
                                           Object op1,
                                           Object op2,
                                           final String operator,
                                           BiPredicate<Long, Long> longComparator,
                                           BiPredicate<Double, Double> doubleComparator,
                                           Predicate<Integer> comparatorTest) {
        // compare them as long if both of them long
        if (Cast.isLong(op1) && Cast.isLong(op2)) {
            return longComparator.test(Cast.toLong(op1), Cast.toLong(op2));
        }

        // compare them as double if at least one is double and cast the other
        if (Cast.isDouble(op1) || Cast.isDouble(op2)) {
            return doubleComparator.test(Cast.toDouble(op1), Cast.toDouble(op2));
        }

        if( op1 instanceof LngObject lngOp1){
            final var method = lngOp1.getField(operator);
            if( method != null ){
                if( !(method instanceof Closure operation)){
                    throw new ExecutionException(String.format("%s is not a valid %s operator", method, operator));
                }
                final var res = operation.callAsMethod(ctx,lngOp1,operator,op2);
                if( !Cast.isBoolean(res)){
                    throw new ExecutionException(String.format("The return value of %s is '%s' cannot be used as a bool", operator, res));
                }
                return Cast.toBoolean(res);
            }
            // if method was null roll over and try comparable, but LngObject is currently not
            // but an embedding may extend LngObject
        }

        final Class<?> comparableClass;
        if (op1.getClass().isAssignableFrom(op2.getClass())) {
            comparableClass = op1.getClass();
        } else if (op2.getClass().isAssignableFrom(op1.getClass())) {
            comparableClass = op2.getClass();
        } else {
            throw new ExecutionException("Cannot compare types %s and %s", op1.getClass(), op2.getClass());
        }
        if (Comparable.class.isAssignableFrom(comparableClass)) {
            @SuppressWarnings("rawtypes") final var cop1 = (Comparable) op1;
            @SuppressWarnings("rawtypes") final var cop2 = (Comparable) op2;
            //noinspection unchecked
            return comparatorTest.test(cop1.compareTo(cop2));
        } else {
            throw new ExecutionException("Cannot compare types of %s", comparableClass);
        }
    }

    @Operator.Symbol("<")
    public static class LessThan extends Compare {
        @Override
        public Object execute(Context ctx, Command left, Command right) throws ExecutionException {
            return compare(ctx, left, right, "<",LONG_LESS_THAN_PREDICATE, DOUBLE_LESS_THAN_PREDICATE, LESS_THAN_COMPARATOR_PREDICATE);
        }
    }

    @Operator.Symbol("<=")
    public static class LessOrEqual extends Compare {
        @Override
        public Object execute(Context ctx, Command left, Command right) throws ExecutionException {
            return compare(ctx, left, right, "<=", LONG_LESS_EQUAL_PREDICATE, DOUBLE_LESS_EQUAL_PREDICATE, LESS_EQUAL_COMPARATOR_PREDICATE);
        }
    }

    @Operator.Symbol(">")
    public static class GreaterThan extends Compare {
        @Override
        public Object execute(Context ctx, Command left, Command right) throws ExecutionException {
            return compare(ctx, left, right, ">", LONG_GREATER_THAN_PREDICATE, DOUBLE_GREATER_THAN_PREDICATE, GREATER_THAN_COMPARATOR_PREDICATE);
        }
    }

    @Operator.Symbol(">=")
    public static class GreaterOrEqual extends Compare {
        @Override
        public Object execute(Context ctx, Command left, Command right) throws ExecutionException {
            return compare(ctx, left, right, ">=", LONG_GREATER_EQUAL_PREDICATE, DOUBLE_GREATER_EQUAL_PREDICATE, GREATER_EQUAL_COMPARATOR_PREDICATE);
        }
    }

    @Operator.Symbol("==")
    public static class Equal extends Compare {
        @Override
        public Object execute(Context ctx, Command left, Command right) throws ExecutionException {
            final var op1 = left.execute(ctx);
            final var op2 = right.execute(ctx);
            if (op1 == null && op2 == null) {
                return true;
            } else if (op1 == null || op2 == null) {
                return false;
            } else {
                return op1.equals(op2);
            }
        }
    }

    @Operator.Symbol("===")
    public static class Same extends Compare {
        @Override
        public Object execute(Context ctx, Command left, Command right) throws ExecutionException {
            final var op1 = left.execute(ctx);
            final var op2 = right.execute(ctx);
            if (Cast.isLong(op1) || Cast.isLong(op2)) {
                return op1.equals(op2);
            }
            if (Cast.isDouble(op1) || Cast.isDouble(op2)) {
                return op1.equals(op2);
            }
            return op1 == op2;
        }
    }

    @Operator.Symbol("!=")
    public static class NotEqual extends Compare {
        @Override
        public Object execute(Context ctx, Command left, Command right) throws ExecutionException {
            final var op1 = left.execute(ctx);
            final var op2 = right.execute(ctx);
            if (op1 == null && op2 == null) {
                return false;
            } else if (op1 == null || op2 == null) {
                return true;
            } else {
                return !op1.equals(op2);
            }
        }
    }

}
