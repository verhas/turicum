package javax0.turicum.commands;

sealed public interface Conditional permits Conditional.Result {
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

        private BreakResult(Object result) {
            super(result, true);
        }

        @Override
        public String toString() {
            return "break{" + result() + "}";
        }
    }

    final class ReturnResult extends Result {
        private ReturnResult(Object result) {
            super(result, true);
        }

        @Override
        public String toString() {
            return "return{" + result() + "}";
        }

    }

    static Conditional doReturn(Object result) {
        return new ReturnResult(result);
    }

    static Conditional doBreak(Object result) {
        return new BreakResult(result);
    }

    static Conditional result(Object result) {
        return new Result(result, false);
    }
}
