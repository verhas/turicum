package ch.turic.memory;

import ch.turic.ExecutionException;

import java.util.ArrayList;
import java.util.List;

/**
 * A special context holding constant string, like built-ins, one for the interpreter
 */
public class ThreadContext {
    private Yielder yielder = null;

    private final List<LngStackFrame> trace = new ArrayList<>();

    public int traceSize() {
        return trace.size();
    }

    public void resetTrace(int size) {
        final var safe = new ArrayList<>(trace);
        trace.clear();
        for (int i = 0; i < size; i++) {
            trace.add(safe.get(i));
        }
    }

    public List<LngStackFrame> getStackTrace() {
        return new ArrayList<>(trace);
    }

    public void push(LngStackFrame frame) {
        trace.add(frame);
    }

    public void pop() {
        trace.removeLast();
    }

    public Yielder yielder() throws ExecutionException {
        return yielder;
    }

    public void addYielder(Yielder yielder) throws ExecutionException {
        this.yielder = yielder;
    }
}
