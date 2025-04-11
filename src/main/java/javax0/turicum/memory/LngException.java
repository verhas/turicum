package javax0.turicum.memory;

import javax0.turicum.ExecutionException;

import java.util.Iterator;
import java.util.List;

public class LngException extends LngObject {
    private static final LngClass exceptionClass = new LngClass(null, null, "EXCEPTION");
    private final Throwable e;
    private final List<StackFrame> stackTrace;

    public LngException(Throwable e, List<StackFrame> stackTrace) {
        super(exceptionClass, null);
        this.e = e;
        this.stackTrace = stackTrace;
    }

    @Override
    public Context context() {
        throw new ExecutionException("Exceptions do not have contex.");
    }

    @Override
    public Object getIndex(Object index) throws ExecutionException {
        throw new ExecutionException("Exeptions do not have index.");
    }

    @Override
    public LngClass lngClass() {
        return exceptionClass;
    }

    @Override
    public void setField(String name, Object value) {
        throw new ExecutionException("Exeptions are immutable.");
    }

    @Override
    public void setIndex(Object index, Object value) throws ExecutionException {
        super.setIndex(index, value);
    }


    @Override
    public Iterator<Object> iterator() {
        return super.iterator();
    }

    @Override
    public Object getField(String name) throws ExecutionException {
        return switch (name) {
            case "stack_trace" -> stackTrace;
            case "message" -> e.getMessage();
            case "cause" -> new LngException(e.getCause(), stackTrace);
            case "supressed" -> {
                final var lngList = new LngList();
                final var supressed = e.getSuppressed();
                for (int i = 0; i < supressed.length; i++) {
                    lngList.setIndex(i, supressed[i]);
                }
                yield lngList;
            }
            default -> null;
        };
    }

    @Override
    public String toString() {
        return e.getMessage();
    }

}
