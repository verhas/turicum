package javax0.turicum.commands;

public sealed interface Conditional permits Conditional.Result {

    boolean isDone();

    Object result();

    sealed class Result implements Conditional permits BreakResult, ReturnResult {

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

    final class BreakResult extends Result {

        private BreakResult(Object result, boolean done) {
            super(result, done);
        }

        @Override
        public String toString() {
            return "break{" + result() + "}";
        }

    }

    final class ReturnResult extends Result {

        private ReturnResult(Object result, boolean done) {
            super(result, done);
        }

        @Override
        public String toString() {
            return "return{" + result() + "}";
        }

    }

    static Conditional doReturn(Object result) {
        return new ReturnResult(result, true);
    }

    static Conditional doBreak(Object result) {
        return new BreakResult(result, true);
    }

    static Conditional result(Object result) {
        return new Result(result, false);
    }

}
