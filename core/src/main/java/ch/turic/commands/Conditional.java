package ch.turic.commands;

import ch.turic.exceptions.ExecutionException;

/**
 * Represents a conditional result that may indicate completion status and hold a value.
 * This interface is implemented by classes that need to represent different types of
 * control flow results, such as break, continue, and return.
 * <p>
 * Such a value is returned by these commands, and the value is handled by other commands following their functionality.
 * A loop may restart or stop executing. A block command will ignore the rest of the commands.
 * These commands interpret the value fetching the real value from the conditional, like a list producing loop appends
 * the value of the 'continue' to the resulting list, a function stops execution and returns the value from the
 * return.
 * <p>
 * If a command has nothing to do with the conditional, then they just return it as they are and higher level commands
 * in the call chain can and will interpret it.
 */
sealed public interface Conditional permits Conditional.Result {
    /**
     * Indicates whether the conditional operation is complete.
     *
     * @return true if the operation is done, false otherwise
     */
    boolean isDone();

    /**
     * Retrieves the result value of this conditional operation.
     *
     * @return the result value, can be null
     */
    Object result();

    /**
     * Base implementation of Conditional that stores a result value and completion status.
     * This class serves as the parent for specific result types like break and return.
     */
    sealed class Result implements Conditional permits BreakResult, ReturnResult, ContinueResult {
        private final Object result;
        private final boolean done;

        private Result(Object result, boolean done) {
            this.result = result;
            this.done = done;
        }

        /**
         * Checks whether the current operation or computation has been marked as completed.
         * The operation was completed if it was executed. It happens when a {@code break}, {@code return}, or {@code continue} statement is encountered
         * without {@code when} condition or the value of the condition is {@code true}.
         * <p>
         * In all other cases these commands return a {@link Result} with {@link #isDone()} false.
         * <p>
         * When a block is executed in a loop it will also return a {@link Result} with {@link #isDone()} false
         * unless it executed a {@code break} or {@code return} statement.
         *
         * @return true if the operation is complete; otherwise, false.
         */
        public boolean isDone() {
            return done;
        }

        public Object result() {
            return result;
        }

        @Override
        public String toString() {
            return "result{" + result() + "}";
        }
    }

    /**
     * Represents a break statement result, typically used in loop control flow.
     */
    final class BreakResult extends Result {

        private BreakResult(Object result, boolean done) {
            super(result, done);
        }

        @Override
        public String toString() {
            return "break{" + result() + "}";
        }
    }

    final class ContinueResult extends Result {

        private ContinueResult(Object result) {
            super(result, true);
        }

        @Override
        public String toString() {
            return "continue{" + result() + "}";
        }
    }

    /**
     * Represents a return statement result, typically used for method returns.
     */
    final class ReturnResult extends Result {
        private ReturnResult(Object result, boolean done) {
            super(result, done);
        }

        @Override
        public String toString() {
            return "return{" + result() + "}";
        }

    }

    private static void noConditional(Object result) {
        if (result instanceof Conditional) {
            throw new ExecutionException("You cannot have a break/continue/return as a value of another one");
        }
    }

    /**
     * Creates a ReturnResult with the specified result value.
     *
     * @param result the value to return
     * @return a new ReturnResult instance
     */
    static Conditional doReturn(Object result) {
        noConditional(result);
        return new ReturnResult(result, true);
    }

    /**
     * Creates a BreakResult with the specified result value.
     *
     * @param result the value to break with
     * @return a new BreakResult instance
     */
    static Conditional doBreak(Object result) {
        noConditional(result);
        return new BreakResult(result, true);
    }

    static Conditional doContinue(Object result) {
        noConditional(result);
        return new ContinueResult(result);
    }

    /**
     * Creates a basic Result with the specified value.
     *
     * @param result the result value
     * @return a new Result instance
     */
    static Conditional result(Object result) {
        return new Result(result, false);
    }
}
