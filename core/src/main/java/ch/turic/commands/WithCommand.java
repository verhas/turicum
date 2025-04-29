package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.Context;
import ch.turic.memory.LngObject;

public class WithCommand extends AbstractCommand {
    public final Command[] objectExpressions;

    public Command body() {
        return body;
    }

    public Command[] objectExpressions() {
        return objectExpressions;
    }

    public WithCommand(Command[] objectExpressions, Command body) {
        this.body = body;
        this.objectExpressions = objectExpressions;
    }

    public final Command body;

    @Override
    public Object _execute(final Context context) throws ExecutionException {
        Object result = null;
        context.step();
        var ctx = context;
        final var objects = new Object[objectExpressions.length];
        for (int i = 0 ; i < objectExpressions.length; i++) {
            final var objectExpression = objectExpressions[i];
            objects[i] = objectExpression.execute(context);
            if (objects[i] instanceof LngObject lngObject) {
                ctx = ctx.with(lngObject.context());
            } else {
                throw new ExecutionException("expression '%s' in 'with' resulted a non-object '%s'", objectExpression, objects[i]);
            }
        }
        // TODO call entry method for all objects forward order
        body.execute(ctx);
        // TODO call exit method for all objects in reverse order
        return result;
    }
}
