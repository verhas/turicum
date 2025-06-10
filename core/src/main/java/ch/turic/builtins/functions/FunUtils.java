package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.LngObject;

import java.util.Arrays;
import java.util.Optional;

/**
 * Utility class containing methods for function argument validation and type checking.
 * This class provides a set of static methods to enforce argument constraints
 * and ensure type correctness within a specific execution context.
 */
public class FunUtils {

    /**
     * Validates that the arguments array is not empty.
     *
     * @param name the name of the function being validated
     * @param args the array of arguments to check
     * @throws ExecutionException if the arguments array is empty
     */
    public static void needArg(final String name, Object[] args) {
        ExecutionException.when(args.length == 0, "Function %s needs at least one argument.", name);
    }

    /**
     * Validates that the function has no arguments.
     *
     * @param name the name of the function being validated
     * @param args the array of arguments to check
     * @throws ExecutionException if there is more than one argument
     */
    public static void noArg(final String name, Object[] args) {
        ExecutionException.when(args.length > 1, "Built-in function %s needs no arguments", name);
    }

    /**
     * Validates that the function has at most one optional argument.
     *
     * @param name the name of the function being validated
     * @param args the array of arguments to check
     * @throws ExecutionException if there is more than one argument
     */
    public static void oneArgOpt(final String name, Object[] args) {
        ExecutionException.when(args.length > 1, "Built-in function %s needs at most one argument", name);
    }

    /**
     * Validates that the function has exactly one argument and returns it.
     *
     * @param name the name of the function being validated
     * @param args the array of arguments to check
     * @return the single argument
     * @throws ExecutionException if there isn't exactly one argument
     */
    public static Object arg(final String name, Object[] args) {
        return arg(name, args, Object.class);
    }

    /**
     * Validates that the function has at least one argument and returns the first one.
     *
     * @param name the name of the function being validated
     * @param args the array of arguments to check
     * @return the first argument
     * @throws ExecutionException if there are no arguments
     */
    public static Object oneOrMoreArgs(final String name, Object[] args) {
        ExecutionException.when(args.length == 0, "Built-in function '%s' needs exactly argument", name);
        return args[0];
    }

    /**
     * Validates that the function has exactly two arguments.
     *
     * @param name the name of the function being validated
     * @param args the array of arguments to check
     * @throws ExecutionException if there aren't exactly two arguments
     */
    public static void twoArgs(final String name, Object[] args) {
        ExecutionException.when(args.length != 2, "Function %s needs two arguments.", name);
    }

    public static void twoPlusArgs(final String name, Object[] args) {
        ExecutionException.when(args.length < 2, "Function %s needs at least two arguments.", name);
    }

    public static class Argument {
        private final String name;
        private final Object value;
        private final int index;
        public final Class<?> type;

        public Argument(String name, Object value, int index) {
            this.name = name;
            this.value = value;
            this.index = index;
            this.type = value == null ? null : value.getClass();
        }

        public Object get() {
            return value;
        }

        public <T> T getOr(T defaultValue) {
            return (T) get();
        }

        public double doubleValue() {
            return ((Number) value).doubleValue();
        }

        public int intValue() {
            return ((Number) value).intValue();
        }

        public <T> T as(Class<T> type) {
            if (!type.isInstance(value)) {
                throw new ExecutionException("Cannot cast argument %d of function '%s' to %s", index, name, type.getSimpleName());
            }
            //noinspection unchecked
            return (T) value;
        }

        public <T> T as(Class<T> type, T defaultValue) {
            if (value == null) {
                return defaultValue;
            }
            if (!type.isInstance(value)) {
                throw new ExecutionException("Cannot cast argument %d of function '%s' to %s", index, name, type.getSimpleName());
            }
            //noinspection unchecked
            return (T) value;
        }

        public <T> Optional<T> optional(Class<T> type) {
            if (value == null) {
                return Optional.empty();
            }
            if (!type.isInstance(value)) {
                throw new ExecutionException("Cannot cast argument %d of function '%s' to %s", index, name, type.getSimpleName());
            }
            //noinspection unchecked
            return Optional.<T>of((T) value);
        }

        public boolean is_a(Class<?> type) {
            return type.isInstance(value);
        }
    }

    private static final Argument UNDEFINED_ARGUMENT = new Argument("undefined", new Object(), 0) {
        @Override
        public <T> T getOr(T defaultValue) {
            return defaultValue;
        }

        public <T> T as(Class<T> type, T defaultValue) {
            return defaultValue;
        }
    };

    public static class ArgumentsHolder {
        private final Argument[] arguments;
        private final Object[] args;
        public final int N;

        public ArgumentsHolder(Object[] arguments, String name) {
            this.args = arguments;
            this.arguments = new Argument[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                this.arguments[i] = new Argument(name, arguments[i], i + 1);
            }
            this.N = arguments.length;
        }

        public Argument at(int i) {
            if (i >= arguments.length) {
                return UNDEFINED_ARGUMENT;
            }
            return arguments[i];
        }

        public Object[] tail(int from) {
            return Arrays.copyOfRange(args, from, N);
        }

        public Argument last() {
            return arguments[arguments.length - 1];
        }

        public Argument first() {
            return arguments[0];
        }

        public interface LngLong {
        }

        public interface LngDouble {
        }

        public interface LngNumber {
        }

        public record Optional<T>(Class<T> type) {
        }

        public static <T> Optional<T> optional(Class<T> type) {
            return new Optional<>(type);
        }

    }

    public static <T> T arg(final String name, Object[] args, Class<T> type) {
        if (args.length != 1) {
            throw new ExecutionException("Built-in function '%s' needs exactly 1 argument.", name);
        }
        //noinspection unchecked
        return (T) args[0];
    }

    public static ArgumentsHolder args(final String name, Object[] args, Object... types) {
        int minArgs = types.length;
        for (int j = types.length - 1; j >= 0; j--) {
            if (!(types[j] instanceof ArgumentsHolder.Optional<?>)) {
                minArgs = j;
                break;
            }
        }
        final int maxArgs = types[types.length - 1] == Object[].class ? Integer.MAX_VALUE : types.length;

        if (args.length < minArgs || args.length > maxArgs) {
            if (minArgs == maxArgs) {
                throw new ExecutionException("Built-in function '%s' needs exactly %s argument%s.", name, minArgs, minArgs == 1 ? "" : "s");
            } else {
                throw new ExecutionException("Built-in function '%s' needs minimum %s and maximum %s arguments.", name, minArgs, maxArgs);
            }
        }
        for (int i = 0; i < types.length; i++) {
            if (types[i] == Object[].class && i == types.length - 1) {
                return new ArgumentsHolder(args, name);
            }
            if (types[i] == null) {
                throw new IllegalArgumentException("Types array contains null element for '%s'.".formatted(name));
            }
            if (types[i] instanceof ArgumentsHolder.Optional(Class<?> type)) {
                if (args.length <= i || args[i] == null) {
                    continue;
                }
                types[i] = type;
            }
            if (!(types[i] instanceof Class<?> klass)) {
                throw new ExecutionException("Types array contains unexpected type for '%s'.".formatted(name));
            }
            if (klass == ArgumentsHolder.LngLong.class) {
                if (Cast.isLong(args[i])) {
                    args[i] = Cast.toLong(args[i]);
                    continue;
                } else {
                    throw new ExecutionException("Built-in function '%s' %s argument needs to be a number",
                            name, nth(i + 1));
                }
            }
            if (klass == ArgumentsHolder.LngDouble.class) {
                if (Cast.isDouble(args[i])) {
                    args[i] = Cast.toDouble(args[i]);
                    continue;
                } else {
                    throw new ExecutionException("Built-in function '%s' %s argument needs to be a floating point number",
                            name, nth(i + 1));
                }
            }
            if (klass == ArgumentsHolder.LngNumber.class) {
                if (Cast.isLong(args[i])) {
                    args[i] = Cast.toLong(args[i]);
                    continue;
                } else if (Cast.isDouble(args[i])) {
                    args[i] = Cast.toDouble(args[i]);
                    continue;
                } else {
                    throw new ExecutionException("Built-in function '%s' %s argument needs to be a floating point or integer number",
                            name, nth(i + 1));
                }
            }
            if (!(klass.isInstance(args[i]))) {
                throw new ExecutionException("Built-in function '%s' %s argument needs to be a %s",
                        name, nth(i + 1), klass.getSimpleName());
            }
        }
        return new ArgumentsHolder(args, name);
    }

    private static String nth(int i) {
        return String.format("%s-%s", i,
                switch (i) {
                    case 1 -> "st";
                    case 2 -> "nd";
                    case 3 -> "rd";
                    default -> "th";
                });
    }


    /**
     * Validates that the function has exactly n arguments.
     *
     * @param name the name of the function being validated
     * @param args the array of arguments to check
     * @param n    the expected number of arguments
     * @throws ExecutionException if there aren't exactly n arguments
     */
    public static void nArgs(final String name, Object[] args, int n) {
        ExecutionException.when(args.length != n, "Function %s needs %d argument.", name, n);
    }

    /**
     * Check that the argument is an LngObject and if it is then return it cast.
     *
     * @param name the name of the function that called this utility method
     * @param arg  the argument that has to be an object
     * @return the argument
     */
    public static LngObject lngObject(final String name, Object arg) {
        if (arg instanceof LngObject) {
            return (LngObject) arg;
        }
        throw new ExecutionException("The argument to '%s()' has to be an object", name);
    }

    /**
     * Validates and casts a Context to ch.turic.memory.Context.
     *
     * @param context the context to validate and cast
     * @return the cast context
     * @throws ExecutionException if the context is not of type ch.turic.memory.Context
     */
    public static ch.turic.memory.Context ctx(Context context) {
        if (context instanceof ch.turic.memory.Context ctx) {
            return ctx;
        }
        throw new ExecutionException("context must be a context of type ch.turic.memory.Context");
    }

}
