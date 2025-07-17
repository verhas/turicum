package ch.turic.commands;

import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.LngCallable;
import ch.turic.memory.*;
import ch.turic.utils.Unmarshaller;

import static ch.turic.commands.Closure.evaluateClosureArguments;
import static ch.turic.commands.Macro.evaluateMacroArguments;

/**
 * An expression that calls a method or a function/closure.
 * <p>
 * This class handles function calls, method invocations, and closure executions. It manages:
 * - Parameter evaluation and passing
 * - Method lookup and invocation
 * - Context management for function execution
 * - Type checking and validation
 * <p>
 * The class supports both positional and named arguments, rest parameters, and closure parameters.
 */
public class FunctionCall extends FunctionCallOrCurry {

    public FunctionCall(Command object, Argument[] arguments) {
        super(object, arguments);
    }

    public static FunctionCall factory(final Unmarshaller.Args args) {
        return new FunctionCall(
                args.command("object"),
                args.get("arguments", Argument[].class));
    }

    @Override
    public Object _execute(final Context context) throws ExecutionException {
        final Command myObject = myFunctionObject(context);
        final Object function;
        if (myObject instanceof FieldAccess fieldAccess) {
            final var obj = LeftValue.toObject(fieldAccess.object().execute(context));
            function = getMethod(context, obj, fieldAccess.identifier());
            if (function instanceof ClosureLike command) {
                final var nullableOptionalResult = command.methodCall(context, obj, fieldAccess.identifier(), this.arguments());
                if (nullableOptionalResult.isPresent()) {
                    return nullableOptionalResult.get();
                }
            }
            if (function instanceof LngClass lngClass) {
                return lngClass.newInstance(obj, context, arguments);
            }
            if (function instanceof LngCallable.LngCallableClosure callable) {
                return callable.call(context, bareValues(evaluateClosureArguments(context, this.arguments)));
            }
            if (function instanceof LngCallable.LngCallableMacro callable) {
                return callable.call(context, bareValues(evaluateMacroArguments(context, this.arguments)));
            }
            throw new ExecutionException("It is not possible to invoke %s.%s() as %s.%s()", obj, function, fieldAccess.object(), fieldAccess.identifier());
        } else {
            function = myObject.execute(context);
            if (function instanceof ClosureLike command) {
                final ArgumentEvaluated[] argValues;
                final int offset;
                if (command.getCurriedArguments() == null) {
                    argValues = new ArgumentEvaluated[this.arguments.length];
                    offset = 0;
                } else {
                    offset = command.getCurriedArguments().length;
                    argValues = new ArgumentEvaluated[this.arguments.length + offset];
                    System.arraycopy(command.getCurriedArguments(), 0, argValues, 0, offset);
                }
                System.arraycopy(command.evaluateArguments(context, this.arguments), 0, argValues, offset, this.arguments.length);
                if (command.getCurriedSelf() != null) {
                    final var obj = command.getCurriedSelf();
                    final var nullableOptionalResult = ClosureLike.callTheMethod(context, obj, command.name(), argValues, command);
                    if (nullableOptionalResult.isPresent()) {
                        return nullableOptionalResult.get();
                    }
                } else {
                    final var ctx = context.wrap(command.wrapped());
                    ctx.setCaller(context);
                    ctx.let0("me", function);
                    ctx.freeze("me");
                    ctx.let0(".", command.name());
                    ctx.freeze(".");
                    defineArgumentsInContext(ctx, context, command.parameters(), argValues, true);
                    return command.execute(ctx);
                }
            }

            if (function instanceof LngClass lngClass) {
                return lngClass.newInstance(null, context, arguments);
            }

            if (function instanceof LngCallable.LngCallableClosure callable) {
                return callable.call(context, bareValues(evaluateClosureArguments(context, this.arguments)));
            }
            if (function instanceof LngCallable.LngCallableMacro callable) {
                return callable.call(context, bareValues(evaluateMacroArguments(context, this.arguments)));
            }
            throw new ExecutionException("It is not possible to invoke '%s' because its value is '%s' and not something I can invoke." +
                    "It is a f5g %s", object, function, function == null ? "null" : function.getClass().getName());
        }
    }
}
