package javax0.turicum.memory;

import javax0.turicum.commands.ExecutionException;

import java.util.Arrays;
import java.util.Objects;

/**
 * Class information in the language
 */
public record LngClass(ClassContext context, String[] parameters, String name) implements HasFields, LngCallable {

    public LngClass(ClassContext context, String[] parameters, String name) {
        this.context = context;
        this.parameters = Objects.requireNonNullElse(parameters, new String[0]);
        this.name = name;
    }

    @Override
    public void setField(String name, Object value) throws ExecutionException {
        context.local(name, value);
    }

    @Override
    public Object getField(String name) throws ExecutionException {
        final var selfField = context.getLocal(name);
        if (selfField != null) {
            return selfField;
        }
        if (context.parents() != null) {
            for (var parent : context.parents()) {
                final var parentField = parent.getField(name);
                if (parentField != null) {
                    return parentField;
                }
            }
        }
        return null;
    }

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var ctx = context.wrap(this.context);
        ExecutionException.when(arguments.length != parameters.length, "Parameter mismatch in constructor");
        for (int i = 0; i < parameters.length; i++) {
            ctx.local(parameters[i], arguments[i]);
        }
        final var object = new LngObject(this, ctx);
        ctx.local("this", object);
        ctx.freeze("this");
        return object;
    }

    @Override
    public String toString() {
        return String.format("class %s(%s):%s}", name, String.join(",",parameters),String.join(",",
                Arrays.stream(context().parents()).map(t -> t.name).toArray(String[]::new)));
    }
}
