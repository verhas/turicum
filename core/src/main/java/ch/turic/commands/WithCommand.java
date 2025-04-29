package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.memory.Context;
import ch.turic.memory.LngObject;

public class WithCommand extends AbstractCommand {
    private static final Object[] NO_PARAMS = new Object[0];
    public final Command[] objectExpressions;

    public Command body() {
        return body;
    }

    public WithCommand(Command[] objectExpressions, Command body) {
        this.body = body;
        this.objectExpressions = objectExpressions;
    }

    public final Command body;

    @Override
    public Object _execute(final Context context) throws ExecutionException {
        context.step();
        var ctx = context;
        final var objects = new LngObject[objectExpressions.length];
        for (int i = 0; i < objectExpressions.length; i++) {
            final var objectExpression = objectExpressions[i];
            final var obj = objectExpression.execute(context);
            if (obj instanceof LngObject lngObject) {
                objects[i] = lngObject;
                final var entry = lngObject.getField("entry");
                final LngObject contextObject;
                if (entry instanceof Closure closure) {
                    final var entryResult = closure.call(context, NO_PARAMS);
                    if (entryResult == null) {
                        contextObject = lngObject;
                    } else {
                        if (entryResult instanceof LngObject lngObjectResult) {
                            contextObject = lngObjectResult;
                        } else {
                            throw new ExecutionException("entry for object '%s' returned a non object '%s'", closure, entryResult);
                        }
                    }
                } else {
                    contextObject = lngObject;
                }
                ctx = ctx.with(contextObject.context());
            } else {
                throw new ExecutionException("expression '%s' in 'with' resulted a non-object '%s'", objectExpression, obj);
            }
        }
        final var result = body.execute(ctx);

        for (int i = objects.length -1 ; i >= 0 ; i --) {
            final var object = objects[i];
            final var entry = object.getField("exit");
            if (entry instanceof Closure closure) {
                closure.call(context, NO_PARAMS);
            }
        }
        return result;
    }
}
