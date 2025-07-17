package ch.turic.commands;

import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.memory.*;
import ch.turic.utils.Unmarshaller;

/**
 * An expression that partially calls a method or a function/closure.
 * It is called currying.
 * <p>
 * The result of the call is a closure or macro that has the curried parameters embedded.
 * <p>
 */
public class FunctionCurrying extends FunctionCallOrCurry {
    public FunctionCurrying(Command object, FunctionCall.Argument[] arguments) {
        super(object, arguments);
    }

    public static FunctionCurrying factory(final Unmarshaller.Args args) {
        return new FunctionCurrying(
                args.command("object"),
                args.get("arguments", FunctionCall.Argument[].class));
    }

    @Override
    public Object _execute(final Context context) throws ExecutionException {
        final Command myObject = myFunctionObject(context);
        final Object function;
        if (myObject instanceof FieldAccess fieldAccess) {
            final var obj = LeftValue.toObject(fieldAccess.object().execute(context));
            function = getMethod(context, obj, fieldAccess.identifier());
            if (function instanceof ClosureLike command) {
                final FunctionCall.ArgumentEvaluated[] argValues = command.evaluateArguments(context, this.arguments);
                return command.curried(obj, argValues);
            }
            throw new ExecutionException("It is not possible to curry %s.%s() as %s.%s()", obj, function, fieldAccess.object(), fieldAccess.identifier());
        } else {
            function = myObject.execute(context);
            if (function instanceof ClosureLike command) {
                final FunctionCall.ArgumentEvaluated[] argValues = command.evaluateArguments(context, this.arguments);
                return command.curried(null, argValues);
            }
            throw new ExecutionException("It is not possible to curry '%s' because its value is '%s' and not something I can curry." +
                    "It is a f5g %s", object, function, function == null ? "null" : function.getClass().getName());
        }
    }

}
