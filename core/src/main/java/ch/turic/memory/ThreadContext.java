package ch.turic.memory;

import ch.turic.ExecutionException;
import ch.turic.memory.debugger.DebuggerContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A special context holding the values for the thread.
 */
public class ThreadContext {
    private Yielder yielder = null;

    private Thread thread;

    public void setThread(Thread thread) {
        this.thread = thread;
    }

    private DebuggerContext debuggerContext;

    public DebuggerContext getDebuggerContext() {
        return debuggerContext;
    }

    public void setDebuggerContext(DebuggerContext debuggerContext) {
        this.debuggerContext = debuggerContext;
    }

    private final List<LngStackFrame> trace = new ArrayList<>();

    public ThreadContext() {

    }
    public ThreadContext(Thread thread) {
        this.thread = thread;
    }

    public int traceSize() {
        return trace.size();
    }

    /**
     * This method is called when an exception is caught.
     * <p>
     * Before this call, the stack trace contains all the elements to the point and stack depth where the exception
     * was caught.
     * This call will reset the stack trace, keeping only those elements that are valid in the calling stack to the
     * level of the call stack where the exception was caught.
     *
     * @param size the size of the stack trace at the level of the exception catch
     */
    public void resetTrace(int size) {
        final var safe = getStackTrace();
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
