package ch.turic.memory;

import ch.turic.ExecutionException;
import ch.turic.LngCallable;
import ch.turic.commands.ClosureOrMacro;
import ch.turic.commands.FunctionCall;
import ch.turic.commands.Macro;

import java.util.Set;

import static ch.turic.commands.FunctionCall.*;

/**
 * Class information in the language
 */
public class LngClass implements HasFields, HasContext, LngCallable.LngCallableClosure {

    final ClassContext context;
    final String name;

    public LngClass(ClassContext context, String name) {
        this.context = context;
        this.name = name;
    }

    public Context context() {
        return context;
    }

    public String name() {
        return name;
    }


    public Object newInstance(Object that, Context callerContext, FunctionCall.Argument[] arguments) {
        final var objectContext = callerContext.wrap(context());
        final var uninitialized = new LngObject(this, objectContext);
        if (that != null) {
            objectContext.local("that", that);
            objectContext.freeze("that");
        }
        objectContext.local("this", uninitialized);
        objectContext.local("cls", this);
        FunctionCall.freezeCls(objectContext);
        final var constructor = context().get("init");
        if (constructor instanceof ClosureOrMacro command) {
            return callConstructor(callerContext, arguments, command, objectContext);
        } else {
            objectContext.freeze("this");
            return uninitialized;
        }
    }

    private Object callConstructor(Context callerContext, Argument[] arguments, ClosureOrMacro command, Context objectContext) {
        if (command instanceof Macro) {
            objectContext.setCaller(callerContext);
        }
        final var argValues = command.evaluateArguments(callerContext, arguments);
        defineArgumentsInContext(objectContext, callerContext, command.parameters(), argValues);
        command.execute(objectContext);
        objectContext.freeze("this");
        return objectContext.get("this");
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

    @Override
    public Set<String> fields() {
        return context.keys();
    }

    /**
     * A class is a callable object, and calling it will create a new instance of the class.
     *
     * @param callerContext the context of the caller used to create the context for the object. Only the global (heap)
     *                      and global and thread context is inherited
     * @param arguments     the evaluated values of the arguments.
     * @return the new object instance initialized
     * @throws ExecutionException if there is some error during the initialization
     */
    @Override
    public Object call(ch.turic.Context callerContext, Object[] arguments) throws ExecutionException {
        if (!(callerContext instanceof Context callerCtx)) {
            throw new RuntimeException("Cannot work with this context implementation. This is an internal error.");
        }
        final var objectContext = callerCtx.wrap(context);
        final var uninitialized = new LngObject(this, objectContext);
        objectContext.local("this", uninitialized);
        objectContext.local("cls", this);
        FunctionCall.freezeCls(objectContext);
        final var constructor = uninitialized.getField("init");
        if (constructor != null) {
            if ((constructor instanceof ClosureOrMacro closure)) {
                closure.execute(objectContext);
            } else {
                throw new ExecutionException("Constructor function is not callable");
            }
        }
        final var object = objectContext.getLocal("this");
        objectContext.freeze("this");
        return object;
    }

    public boolean assignableTo(LngClass other) {
        if (other == this) {
            return true;
        }
        for (final var parent : context.parents()) {
            if (parent.assignableTo(other)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("class %s", name);
    }
}
