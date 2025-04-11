package javax0.turicum.memory;

import javax0.turicum.ExecutionException;

import java.util.ArrayList;
import java.util.List;

/**
 * A special context holding constant string, like built-ins, one for the interpreter
 */
public class ThreadContext {
    private final List<Yielder> yielders = new ArrayList<>();

    private final List<StackFrame> trace = new ArrayList<>();

    public int traceSize() {
        return trace.size();
    }

    public void resetTrace(int size){
        final var safe = new ArrayList<>(trace);
        trace.clear();
        for(int i = 0; i < size; i++){
            trace.add(safe.get(i));
        }
    }

    public List<StackFrame> getStackTrace() {
        final var copy = new ArrayList<>(trace);
        return copy;
    }

    public void push(StackFrame frame) {
        trace.add(frame);
    }

    public void pop() {
        trace.removeLast();
    }

    public Yielder currentYielder() throws ExecutionException {
        if (yielders.isEmpty()) {
            throw new ExecutionException("No Yielder available");
        }
        return yielders.getLast();
    }

    public ContextYielder addYielder(Yielder yielder) throws ExecutionException {
        return new ContextYielder(yielder);
    }

    public class ContextYielder implements Yielder, AutoCloseable {
        private final Yielder yielder;

        public ContextYielder(Yielder yielder) {
            this.yielder = yielder;
        }

        @Override
        public void close() throws Exception {
            if (yielders.isEmpty()) {
                throw new ExecutionException("No Yielder to close");
            }
            yielders.removeLast();
        }

        @Override
        public void send(Object o) throws ExecutionException {
            yielder.send(o);
        }

        @Override
        public Object[] collect() {
            return yielder.collect();
        }
    }

}
