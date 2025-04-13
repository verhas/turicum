package ch.turic.memory;

import ch.turic.ExecutionException;
import ch.turic.analyzer.Pos;
import ch.turic.commands.AbstractCommand;

import java.util.Iterator;
import java.util.List;

public class LngException extends LngObject {
    private static final LngClass exceptionClass = new LngClass(null,  "EXCEPTION");
    private final Throwable e;
    private final LngList stackTrace;
    private final Context context;

    public LngException(Context context, Throwable e, List<StackFrame> stackTrace) {
        super(exceptionClass, null);
        this.context = context;
        this.e = e;
        this.stackTrace = new LngList();
        int index = 0;
        for (final var st : stackTrace.reversed()) {
            final var tr = new LngObject(null, context().open());
            AbstractCommand command = st.command();
            tr.setField("command", command.getClass().getSimpleName());
            Pos position = command.startPosition();
            if (position != null) {
                tr.setField("file", position.file);
                tr.setField("line", position.line);
                tr.setField("column", position.column);
                tr.setField("source", position.lines[position.line - 1]);
            } else {
                tr.setField("file", "-");
                tr.setField("line", 0);
                tr.setField("column", 0);
                tr.setField("source", "");
            }
            this.stackTrace.setIndex(index++, tr);
        }
    }

    @Override
    public Context context() {
        return context;
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
            case "cause" -> new LngException(context, e.getCause(), context.threadContext.getStackTrace());
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
