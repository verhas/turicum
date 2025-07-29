package ch.turic.commands;

/**
 * Represents a conditional result that may indicate completion status and hold a value.
 * This interface is implemented by classes that need to represent different types of
 * control flow results, such as breaks and returns.
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

    /**
     * Creates a ReturnResult with the specified result value.
     *
     * @param result the value to return
     * @return a new ReturnResult instance
     */
    static Conditional doReturn(Object result) {
        return new ReturnResult(result, true);
    }

    /**
     * Creates a BreakResult with the specified result value.
     *
     * @param result the value to break with
     * @return a new BreakResult instance
     */
    static Conditional doBreak(Object result) {
        return new BreakResult(result, true);
    }

    static Conditional doContinue(Object result) {
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
