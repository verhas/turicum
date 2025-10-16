package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.exceptions.ExecutionException;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.LngObject;
import ch.turic.memory.LocalContext;

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
        ExecutionException.when(args.length > 0, "Built-in function %s needs no arguments", name);
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

    public static class Argument {
        private final String name;
        private final Object value;
        private final int index;
        public final Class<?> type;

        /**
         * Constructs an {@code Argument} instance with the specified name, value, and index.
         *
         * @param name  The name of the argument. Cannot be null and serves as an identifier for the argument.
         * @param value The value of the argument. Can be of any object type.
         * @param index The zero-based index of the argument in the argument list.
         */
        public Argument(String name, Object value, int index) {
            this.name = name;
            this.value = value;
            this.index = index;
            this.type = value == null ? null : value.getClass();
        }

        /**
         * Retrieves the value associated with this argument.
         * <p>
         * If you want to get a typed value, use {@link #as(Class)}
         *
         * @return the value of the argument, which can be of any object type, or null if no value is set.
         */
        public Object get() {
            return value;
        }

        /**
         * Returns the value of the argument if it is not null, or the provided default value otherwise.
         * If the value is not compatible with the type of the default value, an {@code ExecutionException} is thrown.
         *
         * @param <T>          The type of the value to retrieve or the default value.
         * @param defaultValue The default value to return if the argument's value is null.
         *                     It is also used to determine the type of the return value.
         * @return The argument's value if it is not null; otherwise, the provided default value.
         * @throws ExecutionException If the value is not compatible with the type of the default value.
         */
        @SuppressWarnings("unchecked")
        public <T> T getOr(T defaultValue) {
            if (value == null) {
                return defaultValue;
            }
            if (!defaultValue.getClass().isInstance(value)) {
                throw new ExecutionException("Cannot cast argument %d of function '%s' to %s", index, name, defaultValue.getClass().getSimpleName());
            }
            //noinspection unchecked
            return (T) value;

        }

        /**
         * Converts the value of the argument to a {@code double}.
         *
         * @return The {@code double} representation of the value associated with this argument.
         * @throws ClassCastException If the value cannot be cast to {@code Number}, or if the
         *                            {@code doubleValue} method cannot be invoked on the value.
         */
        public double doubleValue() {
            return ((Number) value).doubleValue();
        }

        /**
         * Converts the value associated with this argument to an {@code int}.
         *
         * @return The {@code int} representation of the value associated with this argument.
         * @throws ClassCastException If the value cannot be cast to {@code Number},
         *                            or if the {@code intValue} method cannot be invoked on the value.
         */
        public int intValue() {
            return ((Number) value).intValue();
        }

        /**
         * Casts the value of this argument to the specified type if possible.
         *
         * @param <T> The target type to cast the value to.
         * @param t   The {@code Class} object representing the type to cast to.
         *            This cannot be {@code null}.
         * @return The value of the argument cast to the specified type.
         * @throws ExecutionException If the value cannot be cast to the specified type.
         */
        public <T> T as(Class<T> t) {
            if (!t.isInstance(value)) {
                throw new ExecutionException("Cannot cast argument %d of function '%s' to %s", index, name, t.getSimpleName());
            }
            //noinspection unchecked
            return (T) value;
        }

        /**
         * Casts the argument's value to the specified type if possible, or returns the provided default value if the value is null.
         * If the value cannot be cast to the specified type, an {@code ExecutionException} is thrown.
         *
         * @param <T>          The target type to cast the value to.
         * @param t            The {@code Class} object representing the type to cast to. This cannot be null.
         * @param defaultValue The default value to return if the argument's value is null. Must be of the same type as the target type.
         * @return The value of the argument cast to the specified type if it is non-null and of the correct type, or the provided default value if the argument's value is null.
         * @throws ExecutionException If the value cannot be cast to the specified type.
         */
        public <T> T as(Class<T> t, T defaultValue) {
            if (value == null) {
                return defaultValue;
            }
            if (!t.isInstance(value)) {
                throw new ExecutionException("Cannot cast argument %d of function '%s' to %s", index, name, t.getSimpleName());
            }
            //noinspection unchecked
            return (T) value;
        }

        /**
         * Attempts to wrap the value of this argument in an {@link Optional}, cast to the specified type.
         *
         * @param <T>  The target type to which the value should be cast.
         * @param type The {@code Class} object representing the desired type. Cannot be null.
         * @return An {@code Optional} containing the value cast to the specified type if the value is non-null
         * and can be cast to the given type, or {@code Optional.empty()} if the value is null.
         * @throws ExecutionException If the value cannot be cast to the specified type.
         */
        public <T> Optional<T> optional(Class<T> type) {
            if (value == null) {
                return Optional.empty();
            }
            if (!type.isInstance(value)) {
                throw new ExecutionException("Cannot cast argument %d of function '%s' to %s", index, name, type.getSimpleName());
            }
            //noinspection unchecked
            return Optional.of((T) value);
        }

        /**
         * Determines whether the current argument is considered present.
         *
         * @return true if the current object is not equal to the constant UNDEFINED_ARGUMENT,
         * false otherwise.
         */
        public boolean isPresent() {
            return this != UNDEFINED_ARGUMENT;
        }

        /**
         * Determines whether the value of this argument is an instance of the specified class type.
         *
         * @param type the {@code Class} object representing the type to check against.
         *             This cannot be {@code null}.
         * @return {@code true} if the value of this argument is an instance of the specified type;
         * {@code false} otherwise.
         */
        public boolean is_a(Class<?> type) {
            return type.isInstance(value);
        }
    }


    /**
     * Represents a special predefined {@link Argument} instance that is considered undefined or invalid.
     * This argument is used as a placeholder and always provides default values when accessed.
     * <p>
     * Key characteristics:
     * 1. Always returns the provided default value for the {@link Argument#getOr(Object)} and {@link Argument#as(Class, Object)} operations.
     * 2. It allows safe handling of cases where an argument is expected but not provided.
     * <p>
     * This constant is primarily used within the utility functions in the containing class to handle
     * cases where an argument is undefined, ensuring that operations can proceed with minimal runtime exceptions.
     */
    private static final Argument UNDEFINED_ARGUMENT = new Argument("undefined", new Object(), 0) {
        @Override
        public <T> T getOr(T defaultValue) {
            return defaultValue;
        }

        public <T> T as(Class<T> t, T defaultValue) {
            return defaultValue;
        }
    };

    public static class ArgumentsHolder {
        /**
         * An array of {@link Argument} objects representing individual arguments encapsulated with their respective
         * metadata such as index, value, and associated function name.
         * <p>
         * This field is initialized during the construction of the {@code ArgumentsHolder} class, wrapping the array
         * of {@code Object} inputs into corresponding {@code Argument} instances. Each argument retains its position
         * (index) and context (name) to facilitate easy access and manipulation.
         * <p>
         * This field is immutable and cannot be modified after initialization.
         */
        private final Argument[] arguments;
        /**
         * Holds an array of arguments used by the {@code ArgumentsHolder} class.
         * Each element in this array can represent an input argument passed to the built-in functions
         * and is utilized for various operations such as indexing, transformations, or subsetting.
         * This field is immutable and initialized during the creation of an {@code ArgumentsHolder} instance.
         */
        private final Object[] args;
        /**
         * The {@code N} field represents the total number of arguments or elements
         * managed within the context of the containing class. It is a constant value
         * that is primarily used to indicate the size or count of a specific array,
         * list, or collection.
         * <p>
         * This field is typically utilized in operations that depend on the number
         * of arguments, such as iterating through or validating elements within
         * the associated collection.
         */
        public final int N;

        /**
         * Constructs an {@code ArgumentsHolder} instance, initializing it with the given array of arguments and a name.
         * Each argument in the input array is wrapped into an {@code Argument} object, with its index and the provided
         * name of the built-in function.
         *
         * @param arguments An array of objects to be stored and wrapped as {@code Argument} instances.
         *                  Each object in the array represents an individual argument.
         * @param name      A name associated with all arguments. It is used to identify the context of the arguments.
         */
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

        /**
         * Returns a portion of the `args` array, starting from the specified index `from` to the end of the array.
         *
         * @param from The starting index (inclusive) from where to copy the elements in the array.
         * @return An array containing the elements from the specified starting index to the end of the original `args` array.
         */
        public Object[] tail(int from) {
            return Arrays.copyOfRange(args, from, N);
        }

        /**
         * Returns the last argument from the list of arguments.
         *
         * @return The last {@code Argument} in the internal arguments array.
         * If the array is empty, accessing this method will throw an {@code ArrayIndexOutOfBoundsException}.
         */
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

    public static int intArg(final String name, Object[] args) {
        if (args.length != 1) {
            throw new ExecutionException("Built-in function '%s' needs exactly one argument.", name);
        }
        if (Cast.isLong(args[0])) {
            return Cast.toLong(args[0]).intValue();
        } else {
            throw new ExecutionException("Argument to %s('%s') must be a number", name, args[0]);
        }
    }

    public static <T> T arg(final String name, Object[] args, Class<T> type) {
        if (args.length != 1) {
            throw new ExecutionException("Built-in function '%s' needs exactly one argument.", name);
        }
        //noinspection unchecked
        return (T) args[0];
    }

    /**
     * Creates an ArgumentsHolder instance after validating the given arguments against the required types.
     *
     * @param name  the name of the function being validated
     * @param args  the array of arguments to validate
     * @param types the expected types of the arguments, supports exact types, number types, or optional types
     * @return an ArgumentsHolder instance with the validated arguments
     * @throws ExecutionException       if the arguments do not match the required minimum or maximum count, or if an argument
     *                                  does not match its expected type
     * @throws IllegalArgumentException if the types array contains null elements
     */
    public static ArgumentsHolder args(final String name, Object[] args, Object... types) {
        int minArgs = types.length;
        for (int j = types.length - 1; j >= 0; j--) {
            if (!(types[j] instanceof ArgumentsHolder.Optional<?>)) {
                minArgs = j;
                break;
            }
        }
        final int maxArgs = types.length > 0 && types[types.length - 1] == Object[].class || types.length == 0 ? Integer.MAX_VALUE : types.length;

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
    public static LocalContext ctx(Context context) {
        if (context instanceof LocalContext ctx) {
            return ctx;
        }
        throw new ExecutionException("context must be a context of type ch.turic.memory.Context");
    }

}
