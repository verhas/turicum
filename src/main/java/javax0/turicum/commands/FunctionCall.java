package javax0.turicum.commands;

import javax0.turicum.memory.Context;
import javax0.turicum.memory.LeftValue;
import javax0.turicum.memory.LngCallable;
import javax0.turicum.memory.LngObject;

/**
 * An expression that calls a method or a function.
 */
public record FunctionCall(Command object, Command[] arguments) implements Command {

    @Override
    public Object execute(final Context context) throws ExecutionException {
        final var arguments = this.arguments == null ? new Object[0] : new Object[this.arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = this.arguments[i].execute(context);
        }
        final Object function;
        final Command myObject = myFunctionObject(context);
        if (myObject instanceof FieldAccess(Command objectCommand, String identifier)) {
            final var obj = LeftValue.toObject(objectCommand.execute(context));
            function = obj.getField(identifier);
            if (function instanceof Closure closure) {
                if (obj instanceof LngObject lngObject) {
                    ExecutionException.when(closure.parameters().length != arguments.length, "The number of parameters does not match the number of arguments");
                    final var ctx = context.wrap(lngObject.context());
                    if(ctx.contains("this")){
                        ctx.freeze("this");// better do not change 'this' inside methods
                    }
                    for (int i = 0; i < arguments.length; i++) {
                        ctx.local(closure.parameters()[i], arguments[i]);
                    }
                    return closure.execute(ctx);
                }
            }
        } else {
            function = myObject.execute(context);
        }
        if (function instanceof Closure closure) {
            ExecutionException.when(closure.parameters().length != arguments.length, "The number of parameters does not match the number of arguments");
            final var ctx = context.wrap(closure.wrapped());
            for (int i = 0; i < arguments.length; i++) {
                ctx.local(closure.parameters()[i], arguments[i]);
            }
            return closure.execute(ctx);

        }
        if (function instanceof LngCallable callable) {
            return callable.call(context, arguments);
        }
        throw new ExecutionException("It is not possible to invoke the value '" + function + "' " + object);
    }
    /**
     * Handle the method calls when there is no preceding "this" in front of the method name.
     * <p>
     * If the {@code object} is an {@link Identifier} with a {@code name}, and the context contains a
     * {@code "this"} reference, the method checks whether the {@code this} object (assumed to be an
     * {@link LngObject}) has a field with the given name. If such a field exists, it returns a
     * {@link FieldAccess} expression referring to {@code this.name}. Otherwise, it returns the original
     * {@code object}.
     * <p>
     * If the {@code object} is not an {@link Identifier} or there is no {@code "this"} in the context,
     * the method returns the original {@code object}.
     *
     * @param context the current evaluation context containing variable bindings, possibly including {@code "this"}
     * @return a {@link Command} representing either a field access on {@code this} or the original object
     */
    private Command myFunctionObject(final Context context) {
        final Command myObject;
        if (object instanceof Identifier(String name) && context.contains("this")) {
            final var thisObject = context.get("this");
            if (thisObject instanceof LngObject lngObject && lngObject.context().contains(name)) {
                myObject = new FieldAccess(new Identifier("this"), name);
            } else {
                myObject = object;
            }
        } else {
            myObject = object;
        }
        return myObject;
    }
}
