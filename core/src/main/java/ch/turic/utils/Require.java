package ch.turic.utils;

import java.util.function.Supplier;

/**
 * Utility class that provides methods for validating conditions.
 * The methods in this class are used to enforce preconditions by checking
 * input arguments and throwing exceptions with custom error messages when conditions are not met.
 * <p>
 * This class is supposed to be used to check conditions that are expected to be met.
 * In other words, when the condition is not met it means that there is some logical error in the code.
 * Calling these methods is a kind of precondition declaration at the start of the method,
 * as well as defensive programming.
 * <p>
 * You can also use the methods to check the return value, but that is less common.
 * <p>
 * This class is not intended to be used to check programming logic.
 * <p>
 * You can disable the internal validation mechanism by calling {@link #off()}.
 * In this case, no check will be done.
 * Also calling {@link #require(Supplier, String)} version will not evaluate the supplier in 'off' mode.
 * <p>
 * Switching the check off is for the whole application. The boolean flag is {@code static} and it can only be
 * changed to off by calling {@link #off()}.
 * <p>
 * The implementation uses the {@link IllegalArgumentException} to throw the exception, and you cannot change it.
 * If you want to throw something else, you need a different utility.
 * <p>
 * Also, the switching on/off is done through a {@code static} flag.
 * This makes the off-switching application-wide (classloader-wide, to be precise).
 * If it does not fit you, use a different utility.
 *
 */
public class Require {

    private static boolean on = true;

    /**
     * Disables the internal validation mechanism by setting the flag
     * that controls validation checks to false.
     * This prevents any further validation logic from being enforced
     * until explicitly re-enabled.
     */
    public static void off() {
        on = false;
    }

    /**
     * Validates a condition provided by a {@link Supplier} and throws an exception if the condition is not met.
     * The validation and the evaluation of the condition occur only when the internal validation flag is enabled.
     *
     * @param condition a {@link Supplier} of a Boolean value that represents the condition to be validated
     * @param message   a custom error message to be included in the exception if the condition is not met
     * @throws IllegalArgumentException if the condition evaluates to {@code false} and validations are enabled
     */
    public static void require(Supplier<Boolean> condition, String message) {
        if (on)
            require(condition.get(), message);
    }

    /**
     * Ensures that a specified condition is {@code true} and throws an IllegalArgumentException
     * with the provided message if the condition evaluates to false. This method is
     * only effective when the internal validation flag is enabled.
     *
     * @param condition the condition to be validated; must be true for the method to pass without exception
     * @param message   the custom error message to be included in the exception if the condition is not met
     * @throws IllegalArgumentException if the condition evaluates to false and internal validations are enabled.
     *                                  The code removes the last stack frame from the exception to make it more readable.
     *                                  That makes the exception look like it was thrown from the method that called this one.
     */
    public static void require(boolean condition, String message) {
        if (on && !condition) {
            final var e = new IllegalArgumentException(message);
            final var stackTrace = e.getStackTrace();
            e.setStackTrace(java.util.Arrays.copyOf(stackTrace, stackTrace.length - 1));
            throw e;
        }
    }

}
