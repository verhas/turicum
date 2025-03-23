package javax0.genai.pl.commands.operators;

import javax0.genai.pl.commands.Command;
import javax0.genai.pl.commands.ExecutionException;
import javax0.genai.pl.memory.Context;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

public abstract class Compare implements Operator {

    private static boolean compare(Context ctx, Command left, Command right,
                                   BiPredicate<Long, Long> longComparator,
                                   BiPredicate<Double, Double> doubleComparator,
                                   Predicate<Integer> comparatorTest) throws ExecutionException {
        final var op1 = left.execute(ctx);
        final var op2 = right.execute(ctx);

        // compare them as long if both of them long
        if (Cast.isLong(op1) && Cast.isLong(op2)) {
            return longComparator.test(Cast.toLong(op1), Cast.toLong(op2));
        }

        // compare them as double if at least one is double and cast the other
        if (Cast.isDouble(op1) || Cast.isDouble(op2)) {
            return doubleComparator.test(Cast.toDouble(op1), Cast.toDouble(op2));
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
            return compare(ctx, left, right, (a, b) -> a < b, (a, b) -> a < b, x -> x < 0);
        }
    }

    @Operator.Symbol("<=")
    public static class LessOrEqual extends Compare {
        @Override
        public Object execute(Context ctx, Command left, Command right) throws ExecutionException {
            return compare(ctx, left, right, (a, b) -> a <= b, (a, b) -> a <= b, x -> x <= 0);
        }
    }

    @Operator.Symbol(">")
    public static class GreaterThan extends Compare {
        @Override
        public Object execute(Context ctx, Command left, Command right) throws ExecutionException {
            return compare(ctx, left, right, (a, b) -> a > b, (a, b) -> a > b, x -> x > 0);
        }
    }

    @Operator.Symbol(">=")
    public static class GreaterOrEqual extends Compare {
        @Override
        public Object execute(Context ctx, Command left, Command right) throws ExecutionException {
            return compare(ctx, left, right, (a, b) -> a >= b, (a, b) -> a >= b, x -> x >= 0);
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

}
