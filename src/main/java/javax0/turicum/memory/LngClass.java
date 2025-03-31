package javax0.turicum.memory;

import javax0.turicum.ExecutionException;
import javax0.turicum.LngCallable;

import java.util.Objects;

/**
 * Class information in the language
 */
public class LngClass implements HasFields, HasContext, LngCallable {

    final ClassContext context;
    final String[] parameters;
    final String name;

    public LngClass(ClassContext context, String[] parameters, String name) {
        this.context = context;
        this.parameters = Objects.requireNonNullElse(parameters, new String[0]);
        this.name = name;
    }

    public Context context() {
        return context;
    }

    public String name() {
        return name;
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
    public Object call(javax0.turicum.Context cntxt, Object[] arguments) throws ExecutionException {
        if (!(cntxt instanceof Context callerCtx)) {
            throw new RuntimeException();
        }
        final var ctx = callerCtx.wrap(context);
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
        return String.format("class %s", name);
    }

}
