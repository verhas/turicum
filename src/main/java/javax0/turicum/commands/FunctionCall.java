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
    public Object execute(Context context) throws ExecutionException {
        final var arguments = this.arguments == null ? new Object[0] : new Object[this.arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = this.arguments[i].execute(context);
        }
        final Object function;
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
        if (myObject instanceof FieldAccess(Command objectCommand, String identifier)) {
            final var obj = LeftValue.toObject(objectCommand.execute(context));
            function = obj.getField(identifier);
            if (function instanceof Closure closure) {
                if (obj instanceof LngObject lngObject) {
                    ExecutionException.when(closure.parameters().length != arguments.length, "The number of parameters does not match the number of arguments");
                    final var ctx = context.wrap(lngObject.context());
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
        throw new ExecutionException("Dunno how to call this function '" + function + "' " + object);
    }
}
