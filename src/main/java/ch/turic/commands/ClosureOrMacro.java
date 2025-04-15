package ch.turic.commands;

import ch.turic.memory.Context;
import ch.turic.memory.HasFields;
import ch.turic.memory.LngClass;
import ch.turic.memory.LngObject;
import ch.turic.utils.NullableOptional;

import static ch.turic.commands.FunctionCall.defineArgumentsInContext;
import static ch.turic.commands.FunctionCall.freezeThisAndCls;

public sealed interface ClosureOrMacro extends Command permits Closure, Macro {
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
        defineArgumentsInContext(ctx, context, it.parameters(), argValues);
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
        }
        defineArgumentsInContext(ctx, context, it.parameters(), argValues);
        return ctx;
    }

    static NullableOptional<Object> callTheMethod(Context context, HasFields obj, String methodName, FunctionCall.ArgumentEvaluated[] argValues, ClosureOrMacro it) {
        if (obj instanceof LngObject lngObject) {
            final Context ctx = ClosureOrMacro.prepareObjectContext(context, lngObject, argValues, it);
            return NullableOptional.of(it.execute(ctx));
        }
        if (obj instanceof LngClass lngClass) {
            final var ctx = ClosureOrMacro.getClassContext(context, methodName, lngClass, argValues, it);
            return NullableOptional.of(it.execute(ctx));
        }
        return NullableOptional.empty();
    }

    ParameterList parameters();

    Context wrapped();

    NullableOptional<Object> methodCall(Context context, HasFields obj, String methodName, FunctionCall.Argument[] arguments);

    FunctionCall.ArgumentEvaluated[] evaluateArguments(Context context, FunctionCall.Argument[] arguments);
}
