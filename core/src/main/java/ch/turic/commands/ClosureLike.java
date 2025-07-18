package ch.turic.commands;

import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.memory.*;
import ch.turic.utils.NullableOptional;

import java.util.Set;

import static ch.turic.commands.FunctionCallOrCurry.defineArgumentsInContext;
import static ch.turic.commands.FunctionCallOrCurry.freezeThisAndCls;

public sealed abstract class ClosureLike extends AbstractCommand implements Command, HasFields, Cloneable permits ChainedClosureOrMacro, ClosureOrMacro {

    private static final Set<String> SPECIAL_VARIABLES = Set.of("this", "cls", "me", "it", ".");

    public abstract ParameterList parameters();

    public abstract Context wrapped();

    public abstract String[] returnType();

    public abstract NullableOptional<Object> methodCall(Context context, HasFields obj, String methodName, FunctionCallOrCurry.Argument[] arguments);

    public abstract FunctionCallOrCurry.ArgumentEvaluated[] evaluateArguments(Context context, FunctionCallOrCurry.Argument[] arguments);

    public abstract String name();

    public ClosureLike clone() throws CloneNotSupportedException {
        return (ClosureLike) super.clone();
    }

    public void setField(String name, Object value) throws ExecutionException {
        throw new ExecutionException("You cannot set fields of a closure or a macro");
    }

    public FunctionCallOrCurry.ArgumentEvaluated[] getCurriedArguments() {
        return curriedArguments;
    }

    public HasFields getCurriedSelf() {
        return curriedSelf;
    }

    public void setCurriedArguments(FunctionCallOrCurry.ArgumentEvaluated[] curriedArguments) {
        this.curriedArguments = curriedArguments;
    }

    public void setCurriedSelf(HasFields curriedSelf) {
        this.curriedSelf = curriedSelf;
    }

    private FunctionCallOrCurry.ArgumentEvaluated[] curriedArguments = null;
    private HasFields curriedSelf = null;

    public ClosureLike curried(final HasFields self, final FunctionCallOrCurry.ArgumentEvaluated[] arguments) {
        if (self != null && getCurriedSelf() != null) {
            throw new ExecutionException("You cannot curry a method to different objects. How could it happen?");
        }
        try {
            final var it = (ClosureLike) this.clone();
            it.setCurriedSelf(self);
            if (it.getCurriedArguments() == null) {
                it.setCurriedArguments(arguments);
            } else {
                final var newCurriedArguments = new FunctionCallOrCurry.ArgumentEvaluated[it.getCurriedArguments().length + arguments.length];
                System.arraycopy(getCurriedArguments(), 0, newCurriedArguments, 0, getCurriedArguments().length);
                System.arraycopy(arguments, 0, newCurriedArguments, getCurriedArguments().length, arguments.length);
                it.setCurriedArguments(newCurriedArguments);
            }
            return it;
        } catch (CloneNotSupportedException e) {
            throw new ExecutionException(e, "cloning failed while currying");
        }
    }

    public void curryThis(final HasFields self, final FunctionCallOrCurry.ArgumentEvaluated[] arguments) {
        if (self != null && curriedSelf != null) {
            throw new ExecutionException("You cannot curry a method to different objects. How could it happen?");
        }
        setCurriedSelf(self);
        if (getCurriedArguments() == null) {
            setCurriedArguments(arguments);
        } else {
            final var newCurriedArguments = new FunctionCallOrCurry.ArgumentEvaluated[getCurriedArguments().length + arguments.length];
            System.arraycopy(getCurriedArguments(), 0, newCurriedArguments, 0, getCurriedArguments().length);
            System.arraycopy(arguments, 0, newCurriedArguments, getCurriedArguments().length, arguments.length);
            setCurriedArguments(newCurriedArguments);
        }
    }


    @Override
    public Set<String> fields() {
        return Set.of("name");
    }

    @Override
    public Object getField(String name) throws ExecutionException {
        if ("name".equals(name)) {
            return name();
        } else {
            throw new ExecutionException("You cannot get field '%s' of a closure or a macro.", name);
        }
    }

    private static Context prepareListContext(Context context, String methodName, LngList lngList, FunctionCallOrCurry.ArgumentEvaluated[] argValues, ClosureLike it) {
        final var fp = lngList.getFieldProvider();
        final Context ctx;
        if (fp instanceof LngObject lngObject) {
            ctx = prepareObjectContext(context, lngObject, methodName, argValues, it);
        } else if (fp instanceof LngClass lngClass) {
            ctx = getClassContext(context, methodName, lngClass, argValues, it);
        } else {
            throw new ExecutionException("List field provider is neither object nor class");
        }
        ctx.let0("it", lngList);
        return ctx;
    }

    private static Context prepareObjectContext(Context context, LngObject lngObject, String methodName, FunctionCallOrCurry.ArgumentEvaluated[] argValues, ClosureLike it) {
        final Context ctx;
        if (it.wrapped() == null) {
            ctx = context.wrap(lngObject.context());
        } else {
            ctx = context.wrap(it.wrapped());
        }
        ctx.let0("this", lngObject);
        ctx.let0("cls", lngObject.lngClass());
        ctx.let0(".", methodName);
        ctx.setCaller(context);
        freezeThisAndCls(ctx);
        ctx.freeze(".");
        defineArgumentsInContext(ctx, context, it.parameters(), argValues, true);
        return ctx;
    }

    private static Context getClassContext(Context context, String methodName, LngClass lngClass, FunctionCallOrCurry.ArgumentEvaluated[] argValues, ClosureLike it) {
        final var ctx = context.wrap(lngClass.context());
        ctx.let0(".", methodName);
        ctx.freeze(".");
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
    static NullableOptional<Object> callTheMethod(Context context, HasFields obj, String methodName, FunctionCallOrCurry.ArgumentEvaluated[] argValues, ClosureLike it) {
        if (obj instanceof LngObject lngObject) {
            final Context ctx = ClosureLike.prepareObjectContext(context, lngObject, methodName, argValues, it);
            return NullableOptional.of(it.execute(ctx));
        }
        if (obj instanceof LngList lngList && lngList.hasFieldProvider()) {
            final Context ctx = ClosureLike.prepareListContext(context, methodName, lngList, argValues, it);
            return NullableOptional.of(it.execute(ctx));
        }
        if (obj instanceof LngClass lngClass) {
            final var ctx = ClosureLike.getClassContext(context, methodName, lngClass, argValues, it);
            if (methodName.equals("init") && context.containsLocal("this")) {
                final var resultObject = it.execute(ctx);
                context.mergeVariablesFrom(ctx, SPECIAL_VARIABLES);
                return NullableOptional.of(resultObject);
            } else {
                return NullableOptional.of(it.execute(ctx));
            }
        }
        return NullableOptional.empty();
    }

}
