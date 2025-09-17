package ch.turic.memory;

import ch.turic.ExecutionException;
import ch.turic.Command;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * A lazy object is an object that is read-only, the fields are all commands, and every time the code reads a field
 * it executes the command and returns the result as the value of the corresponding attribute.
 */
public class LazyObject extends LngObject {
    private static final LngClass macroObjectClass = new LngClass(null,  "EXCEPTION");

    /**
     * Create a new closure object.
     *
     * @param context is the surrounding context in which the closure object is created.
     */
    public LazyObject(LocalContext context, Map<String, Command> fields) {
        super(macroObjectClass, context.wrap());
        fields.forEach((key, value) -> context().let0(key, value));

    }

    @Override
    public Object getIndex(Object index) throws ExecutionException {
        throw new ExecutionException("Closure objects do not have index.");
    }

    @Override
    public LngClass lngClass() {
        return macroObjectClass;
    }

    @Override
    public void setField(String name, Object value) {
        throw new ExecutionException("lazy objects are immutable.");
    }

    @Override
    public void setIndex(Object index, Object value) throws ExecutionException {
        throw new ExecutionException("lazy objects are immutable.");
    }

    @Override
    public Object getField(String name) throws ExecutionException {
        final var value = context().get(name);
        if (value instanceof Command command) {
            return command.execute(context());
        } else {
            throw new ExecutionException("Macro object contains a non-command field.");
        }
    }

    @Override
    public String toString() {
        return "{" + context().keys().stream().map(k -> k + "=" + context().get(k)).collect(Collectors.joining(",")) + "}";
    }

}
