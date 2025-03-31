package javax0.turicum.memory;

import javax0.turicum.ExecutionException;
import javax0.turicum.LngCallable;
import javax0.turicum.commands.ClosureOrMacro;

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
    public Object call(javax0.turicum.Context callerContext, Object[] arguments) throws ExecutionException {
        if (!(callerContext instanceof Context callerCtx)) {
            throw new RuntimeException("Cannot work with this context implementation. This is an internal error.");
        }
        final var objectContext = callerCtx.wrap(context);
        ExecutionException.when(arguments.length != parameters.length, "Parameter mismatch in constructor");
        for (int i = 0; i < parameters.length; i++) {
            objectContext.local(parameters[i], arguments[i]);
        }
        final var uninitialized = new LngObject(this, objectContext);
        objectContext.local("this", uninitialized);
        final var object = switch (uninitialized.getField("constructor")) {
            case null -> uninitialized;
            case ClosureOrMacro closure -> {
                if (closure.parameters().length != 0) {
                    throw new ExecutionException("constructor must be parameter less");
                }
                yield closure.execute(objectContext);
            }
            default -> throw new IllegalStateException("Unexpected value: " + uninitialized.getField("constructor"));
        };
        objectContext.local("this", object);
        objectContext.freeze("this");
        return object;
    }

    @Override
    public String toString() {
        return String.format("class %s", name);
    }
}
