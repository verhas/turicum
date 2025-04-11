package javax0.turicum.commands;

import javax0.turicum.ExecutionException;
import javax0.turicum.LngCallable;
import javax0.turicum.memory.*;

/**
 * An expression that calls a method or a function/closure.
 *
 * @param object    is the closure or something that is to be called. It can be {@link LngCallable}, {@link Closure},
 *                  {@link Macro}
 * @param arguments are the arguments of the function call to be evaluated or passed to the implementation if the
 *                  object is a {@link Macro}
 */
public record FunctionCall(Command object, Argument[] arguments) implements Command {
    public record Argument(Identifier id, Command expression) {
    }

    public record ArgumentEvaluated(Identifier id, Object value) {
    }


    @Override
    public Object execute(final Context context) throws ExecutionException {
        final Command myObject = myFunctionObject(context);
        final Object function;
        if (myObject instanceof FieldAccess(Command objectCommand, String identifier)) {
            final var obj = LeftValue.toObject(objectCommand.execute(context));
            function = getMethod(context, obj, identifier);
            if (function instanceof ClosureOrMacro command) {
                final var argValues = switch (command) {
                    case Closure ignored -> evaluateArguments(context);
                    case Macro ignored -> passArgumentsUnevaluated();
                };
                if (obj instanceof LngObject lngObject) {
                    ExecutionException.when(command.parameters().parameters().length != argValues.length, "The number of parameters does not match the number of arguments");
                    final Context ctx;
                    if (command.wrapped() == null) {
                        ctx = context.wrap(lngObject.context());
                    } else {
                        ctx = context.wrap(command.wrapped());
                    }
                    ctx.let0("this", lngObject);
                    ctx.let0("cls", lngObject.lngClass());
                    if( command instanceof Macro) {
                        ctx.setCaller(context);
                    }
                    freezeThisAndCls(ctx);
                    defineArgumentsInContext(ctx, command.parameters(), argValues);
                    return command.execute(ctx);
                }
                if (obj instanceof LngClass lngClass) {
                    ExecutionException.when(command.parameters().parameters().length != argValues.length, "The number of parameters does not match the number of arguments");
                    final var ctx = context.wrap(lngClass.context());
                    if ("constructor".equals(identifier)) {
                        // this will make in a chained constructor call set 'this' to the object created
                        // 'cls' point to the class, but 'this.cls' point to the class which is going to be initialized
                        ctx.let0("this", context.getLocal("this"));
                        ctx.let0("cls", lngClass);
                        freezeThisAndCls(ctx);
                    }
                    defineArgumentsInContext(ctx, command.parameters(), argValues);
                    return command.execute(ctx);
                }
            }
            if (function instanceof LngCallable callable) {
                return callable.call(context, bareValues(evaluateArguments(context)));
            }
            throw new ExecutionException("It is not possible to invoke %s.%s() as %s.%s()", obj, function, objectCommand, identifier);
        } else {
            function = myObject.execute(context);
            if (function instanceof ClosureOrMacro command) {
                final ArgumentEvaluated[] argValues = switch (command) {
                    case Closure ignored -> evaluateArguments(context);
                    case Macro ignored -> passArgumentsUnevaluated();
                };
                final var ctx = context.wrap(command.wrapped());
                defineArgumentsInContext(ctx, command.parameters(), argValues);
                if( command instanceof Macro) {
                    ctx.setCaller(context);
                }
                return command.execute(ctx);

            }
            if (function instanceof LngCallable callable) {
                return callable.call(context, bareValues(evaluateArguments(context)));
            }
            throw new ExecutionException("It is not possible to invoke '%s' because it is '%s'", object, function);
        }
    }

    private Object[] bareValues(FunctionCall.ArgumentEvaluated[] arguments) {
        final Object[] result = new Object[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            result[i] = arguments[i].value;
        }
        return result;
    }

    /**
     * Assign the string to the parameter names in the context provided.
     *
     * @param ctx       the context that will hold the string
     * @param pList     the names of the parameters/arguments
     * @param argValues the array holding the actual argument string
     */
    public static void defineArgumentsInContext(Context ctx, ParameterList pList, ArgumentEvaluated[] argValues) {
        int positionalsIndex = 0;
        final var filled = new boolean[pList.parameters().length];
        final var rest = new LngList();
        final var meta = new LngObject(null, ctx.open());
        Object closure = null;
        for (int i = 0; i < argValues.length; i++) {
            if (i == argValues.length - 1 && pList.closure() != null) {
                closure = argValues[i].value;
                break;
            }
            if (argValues[i].id == null) {
                positionalsIndex = addPositionalParameter(ctx, pList, argValues[i], positionalsIndex, rest, filled);
            } else {
                addNamedParameter(ctx, pList, argValues[i], meta, filled);
            }
        }
        for (int i = 0; i < pList.parameters().length; i++) {
            if (!filled[i]) {
                final var parameter = pList.parameters()[i];
                if (parameter.defaultExpression() == null) {
                    throw new ExecutionException("Parameter '%s' is not defined", parameter.identifier());
                } else {
                    final var value = parameter.defaultExpression().execute(ctx);
                    ctx.defineTypeChecked(parameter.identifier(), value, parameter.types());
                }
            }
        }
        if (pList.rest() != null) {
            ctx.let0(pList.rest(), rest);
        }
        if (pList.meta() != null) {
            ctx.let0(pList.meta(), meta);
        }
        if (pList.closure() != null) {
            ctx.let0(pList.closure(), closure);
        }
    }

    /**
     * Add a named parameter. If the parameter with the given name is a positional only then add the value to the
     * 'meta' parameter object if there is a meta or throw exception.
     *
     * @param ctx      the context to create the variable
     * @param pList    the descriptor of the parameters
     * @param argValue the argument values
     * @param meta     the object holding the extra named parameters
     * @param filled   the array keeping track of which parameters had got value from the caller
     * @throws ExecutionException if there is no 'meta' and the name is not defined
     */
    private static void addNamedParameter(Context ctx, ParameterList pList, ArgumentEvaluated argValue, LngObject meta, boolean[] filled) {
        for (int j = 0; j < pList.parameters().length; j++) {
            final var parameter = pList.parameters()[j];
            if (parameter.identifier().equals(argValue.id.name())) {
                if (parameter.type() == ParameterList.Parameter.Type.POSITIONAL_ONLY) {
                    if (pList.meta() != null) {
                        meta.setField(argValue.id.name(), argValue.value);
                        return;
                    }
                    throw new ExecutionException(
                            "The parameter '%s' is positional only, specified by name and there is no {meta} parameter.",
                            parameter.identifier());
                }
                if (filled[j]) {
                    throw new ExecutionException("Parameter '%s' is already defined", argValue.id.name());
                }
                filled[j] = true;
                ctx.defineTypeChecked(parameter.identifier(), argValue.value, parameter.types());
                return;
            }
        }
        if (pList.meta() != null) {
            meta.setField(argValue.id.name(), argValue.value);
            return;
        }
        throw new ExecutionException("The parameter '%s' is not defined and there is no {meta} parameter", argValue.id.name());
    }

    /**
     * Add a positional parameter to the next non-named only free positional parameter.
     *
     * @param ctx      the context to create the variable
     * @param pList    the descriptor of the parameters
     * @param argValue the argument values
     * @param index    the index of the first fill-able argument (may not be positional)
     * @param rest     the rest object
     * @param filled   the array keeping track of which parameters had got value from the caller
     * @return the next index
     * @throws ExecutionException if there is no rest and there are too many positional parameters
     */
    private static int addPositionalParameter(Context ctx, ParameterList pList, ArgumentEvaluated argValue, int index, LngList rest, boolean[] filled) {
        while (true) {
            if (index >= pList.parameters().length) {
                if (pList.rest() == null) {
                    throw new ExecutionException("Too many parameters and there is no [rest] specified");
                }
                rest.array.add(argValue.value);
                break;
            }
            if (pList.parameters()[index].type() != ParameterList.Parameter.Type.NAMED_ONLY && !filled[index]) {
                ctx.defineTypeChecked(pList.parameters()[index].identifier(), argValue.value, pList.parameters()[index].types());
                filled[index++] = true;
                break;
            }
            index++;
        }
        return index;
    }

    /**
     * Freeze the variable "this" and "cls" in the context.
     *
     * @param ctx the context in which we have to freeze "this"
     */
    public static void freezeThisAndCls(Context ctx) {
        if (ctx.contains("this")) {
            ctx.freeze("this");// better do not change 'this' inside methods
        }
        freezeCls(ctx);
    }

    /**
     * Freeze only the "cls" object when it is a constructor, then 'this' is not frozen.
     *
     * @param ctx the context in which to freeze 'cls'
     */
    public static void freezeCls(Context ctx) {
        if (ctx.contains("cls")) {
            ctx.freeze("cls");// better do not change 'this' inside methods
        }
    }

    private ArgumentEvaluated[] passArgumentsUnevaluated() {
        final var argValues = this.arguments == null ? new ArgumentEvaluated[0] : new ArgumentEvaluated[this.arguments.length];
        for (int i = 0; i < argValues.length; i++) {
            argValues[i] = new ArgumentEvaluated(this.arguments[i].id, this.arguments[i].expression);
        }
        return argValues;
    }

    private ArgumentEvaluated[] evaluateArguments(Context context) {
        final var argValues = this.arguments == null ? new ArgumentEvaluated[0] : new ArgumentEvaluated[this.arguments.length];
        for (int i = 0; i < argValues.length; i++) {
            argValues[i] = new ArgumentEvaluated(this.arguments[i].id, this.arguments[i].expression.execute(context));
        }
        return argValues;
    }

    /**
     * Get the method from the object. If it is a JavaObject, but the contained object has a TuriClass implementing
     * functionality, then get that functionality instead.
     *
     * @param context    the context to get access to the interpreter and through that to the registered TuriClasses
     * @param obj        the object for which we are searching the method
     * @param identifier the name of the method
     * @return the method object that can be a closure
     */
    private static Object getMethod(Context context, HasFields obj, String identifier) {
        return switch (obj) {
            case JavaObject jo -> {
                final var turi = context.globalContext.getTuriClass(jo.object().getClass());
                if (turi != null) {
                    yield turi.getMethod(jo.object(), identifier);
                }
                yield jo.getField(identifier);
            }
            default -> obj.getField(identifier);
        };
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
            if (thisObject instanceof LngObject lngObject && lngObject.context().containsLocal(name)) {
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
