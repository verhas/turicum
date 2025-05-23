package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.memory.Context;
import ch.turic.memory.HasFields;
import ch.turic.memory.LngClass;
import ch.turic.memory.LngObject;
import ch.turic.utils.NullableOptional;

import java.util.Set;

import static ch.turic.commands.FunctionCall.defineArgumentsInContext;
import static ch.turic.commands.FunctionCall.freezeThisAndCls;

public sealed interface ClosureOrMacro extends Command, HasFields permits Closure, Macro {
    static Context prepareObjectContext(Context context, LngObject lngObject, FunctionCall.ArgumentEvaluated[] argValues, ClosureOrMacro it) {
        final Context ctx;
        if (it.wrapped() == null) {
            ctx = context.wrap(lngObject.context());
        } else {
            ctx = context.wrap(it.wrapped());
        }
        ctx.let0("this", lngObject);
        ctx.let0("cls", lngObject.lngClass());
        ctx.setCaller(context);
        freezeThisAndCls(ctx);
        defineArgumentsInContext(ctx, context, it.parameters(), argValues, true);
        return ctx;
    }

    static Context getClassContext(Context context, String methodName, LngClass lngClass, FunctionCall.ArgumentEvaluated[] argValues, ClosureOrMacro it) {
        final var ctx = context.wrap(lngClass.context());
        if ("init".equals(methodName)) {
            // this will make in a chained constructor call set 'this' to the object created
            // 'cls' point to the class, but 'this.cls' point to the class which is going to be initialized
            ctx.let0("this", context.getLocal("this"));
            ctx.let0("cls", lngClass);
            freezeThisAndCls(ctx);
            defineArgumentsInContext(ctx, context, it.parameters(), argValues, false);
        } else {
            defineArgumentsInContext(ctx, context, it.parameters(), argValues, true);
        }
        return ctx;
    }

    /**
     * Calls a specified method on the provided object or class within a given execution context.
     * The method execution is influenced by the provided arguments and the closure or macro logic.
     * If the object is an instance of {@code LngObject}, it invokes the method in the object context.
     * If the object is an instance of {@code LngClass}, it invokes the method in the class context.
     * If neither condition is met, an empty {@code NullableOptional} is returned.
     *
     * @param context    the execution context in which the method is called
     * @param obj        the target object or class (must implement {@code HasFields}) on which the method is invoked
     * @param methodName the name of the method to be called
     * @param argValues  the pre-evaluated arguments to be passed to the method
     * @param it         the closure or macro defining the method execution logic
     * @return a {@code NullableOptional} containing the result of the method execution if successful,
     * or an empty {@code NullableOptional} if the method invocation is not applicable
     */
    static NullableOptional<Object> callTheMethod(Context context, HasFields obj, String methodName, FunctionCall.ArgumentEvaluated[] argValues, ClosureOrMacro it) {
        if (obj instanceof LngObject lngObject) {
            final Context ctx = ClosureOrMacro.prepareObjectContext(context, lngObject, argValues, it);
            return NullableOptional.of(it.execute(ctx));
        }
        if (obj instanceof LngClass lngClass) {
            final var ctx = ClosureOrMacro.getClassContext(context, methodName, lngClass, argValues, it);
            if (methodName.equals("init") && context.containsLocal("this")) {
                final var resultObject = it.execute(ctx);
                exportFromParentFrame(ctx, context);
                return NullableOptional.of(resultObject);
            } else {
                return NullableOptional.of(it.execute(ctx));
            }
        }
        return NullableOptional.empty();
    }

    private static void exportFromParentFrame(Context parent, Context to) {
        for (final var variable : parent.allFrameKeys()) {
            switch (variable) {
                case "this":
                case "cls":
                    break;
                default:
                    to.local(variable, parent.get(variable));
                    break;
            }
        }
    }

    ParameterList parameters();

    Context wrapped();

    NullableOptional<Object> methodCall(Context context, HasFields obj, String methodName, FunctionCall.Argument[] arguments);

    FunctionCall.ArgumentEvaluated[] evaluateArguments(Context context, FunctionCall.Argument[] arguments);

    String name();

    default void setField(String name, Object value) throws ExecutionException {
        throw new ExecutionException("You cannot set fields of a closure or a macro");
    }

    @Override
    default Set<String> fields() {
        return Set.of("name");
    }

    default Object getField(String name) throws ExecutionException {
        return switch (name) {
            case "name" -> name();
            default -> throw new ExecutionException("You cannot get field '%s' of a closure or a macro.", name);
        };
    }

}
