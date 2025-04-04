package javax0.turicum.memory;

import javax0.turicum.ExecutionException;
import javax0.turicum.LngCallable;
import javax0.turicum.commands.ClosureOrMacro;
import javax0.turicum.commands.FunctionCall;

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
        final var fieldInSelf = context.getLocal(name);
        if (fieldInSelf != null) {
            return fieldInSelf;
        }
        if (context.parents() != null) {
            for (var parent : context.parents()) {
                final var fieldInParent = parent.getField(name);
                if (fieldInParent != null) {
                    return fieldInParent;
                }
            }
        }
        return null;
    }

    /**
     * A class is a callable object, and calling it will create a new instance of the class.
     *
     * @param callerContext the context of the caller used to create the context for the object. Only the global (heap)
     *                      and global and thread context is inherited
     * @param arguments the evaluated values of the arguments.
     * @return the new object instance initialized
     * @throws ExecutionException if there is some error during the initialization
     */
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
        objectContext.local("cls", this);
        FunctionCall.freezeCls(objectContext);
        final var constructor = uninitialized.getField("constructor");
        if (constructor != null) {
            if ((constructor instanceof ClosureOrMacro closure) && closure.parameters().length == 0) {
                closure.execute(objectContext);
            } else {
                throw new ExecutionException("Constructor function has parameters or uncallable");
            }
        }
        final var object = objectContext.getLocal("this");
        objectContext.freeze("this");
        return object;
}

@Override
public String toString() {
    return String.format("class %s", name);
}
}
