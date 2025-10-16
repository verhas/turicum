package ch.turic.commands;

import ch.turic.Command;
import ch.turic.exceptions.ExecutionException;
import ch.turic.TuriClass;
import ch.turic.builtins.classes.TuriNone;
import ch.turic.memory.*;
import ch.turic.utils.Unmarshaller;

import java.util.Arrays;

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
public abstract class FunctionCallOrCurry extends AbstractCommand {
    public static final String[] EMPTY_STRING_ARRAY = new String[0];
    /*
     * An object is the closure or something that is to be called. It can be {@link LngCallable}, {@link Closure},
     * {@link Macro}
     */
    public final Command object;

    public Argument[] arguments() {
        return arguments;
    }

    public Command object() {
        return object;
    }

    public FunctionCallOrCurry(Command object, Argument[] arguments) {
        this.arguments = arguments;
        this.object = object;
    }

    /*
     * arguments are the arguments of the function call to be evaluated or passed to the implementation if the
     * object is a {@link Macro}
     */
    public final Argument[] arguments;


    public record Argument(Identifier id, Command expression) {
        public static Argument factory(final Unmarshaller.Args args) {
            return new Argument(
                    args.get("id", Identifier.class),
                    args.command("expression"));
        }
    }

    public record ArgumentEvaluated(Identifier id, Object value) {
    }

    /**
     * Extracts the values from an array of evaluated arguments. Converts the evaluated arguments to an object array
     * that contains only the values and not the argument names.
     * <p>
     * Evaluated arguments contain optional names when arguments are passed by name. This method strips these names off
     * to call methods of types that do not feature named arguments.
     *
     * @param arguments an array of {@code FunctionCallOrCurry.ArgumentEvaluated} instances,
     *                  each containing an identifier and a value
     * @return an array of {@code Object}s representing the extracted values from the input arguments
     */
    protected Object[] bareValues(FunctionCallOrCurry.ArgumentEvaluated[] arguments) {
        final Object[] result = new Object[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            result[i] = arguments[i].value;
        }
        return result;
    }

    /**
     * Defines arguments in the execution context based on the parameter list and provided argument values.
     * <p>
     * This method handles the complex task of mapping function call arguments to parameters, including:
     * - Positional and named parameter assignment
     * - Default value handling for unspecified parameters
     * - Rest parameter collection
     * - Meta parameter object population
     * - Closure parameter handling
     *
     * @param ctx           the context where parameters will be defined
     * @param callerContext the context of the caller (used for default value evaluation)
     * @param pList         parameter list definition containing parameter names, types, and attributes
     * @param argValues     array of evaluated arguments to be assigned
     * @param freeze        requires that the arguments are frozen after they are defined
     * @throws ExecutionException if required parameters are missing or parameter assignment fails
     */
    public static void defineArgumentsInContext(final LocalContext ctx,
                                                final LocalContext callerContext,
                                                final ParameterList pList,
                                                final ArgumentEvaluated[] argValues,
                                                final boolean freeze) {
        final var filled = new boolean[pList.parameters().length];
        final var rest = new LngList();
        final var meta = LngObject.newEmpty(ctx);
        Object closure = null;
        for (int i = 0; i < argValues.length; i++) {
            final var arg = argValues[i];
            if (i == argValues.length - 1 && pList.closure() != null && arg.id() == null) {
                boolean allMadatoryPositionalsDone = true;
                for (int j = 0; j < pList.parameters().length; j++) {
                    if (pList.parameters()[j].type() != ParameterList.Parameter.Type.NAMED_ONLY) {
                        allMadatoryPositionalsDone = allMadatoryPositionalsDone && filled[j];
                    }
                }
                if (allMadatoryPositionalsDone) {
                    closure = arg.value;
                    break;
                }
            }
            if (argValues[i].id == null) {
                addPositionalParameter(ctx, pList, arg, rest, meta, filled, freeze);
            } else {
                addNamedParameter(ctx, pList, arg, meta, filled, false);
            }
        }
        for (int i = 0; i < pList.parameters().length; i++) {
            if (!filled[i]) {
                final var parameter = pList.parameters()[i];
                if (parameter.defaultExpression() == null) {
                    throw new ExecutionException("Parameter '%s' is not defined", parameter.identifier());
                } else {
                    final var value = parameter.defaultExpression().execute(callerContext);
                    ctx.defineTypeChecked(parameter.identifier(), value, calculateTypeNames(ctx, parameter.types()));
                    if (freeze) {
                        ctx.freeze(parameter.identifier());
                    }
                }
            }
        }
        if (pList.rest() != null) {
            ctx.let0(pList.rest(), rest);
            if (freeze) {
                ctx.freeze(pList.rest());
            }
        }
        if (pList.meta() != null) {
            ctx.let0(pList.meta(), meta);
            if (freeze) {
                ctx.freeze(pList.meta());
            }
        }
        if (pList.closure() != null) {
            ctx.let0(pList.closure(), closure);
            if (freeze) {
                ctx.freeze(pList.closure());
            }
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
     * @param lenient  do not care if a named value does not have a corresponding parameter. This is used when an object
     *                 is spread. In that case, it is not a problem, if there is no "cls", "this" or some other parameters-
     * @throws ExecutionException if there is no 'meta' and the name is not defined
     */
    protected static void addNamedParameter(LocalContext ctx, ParameterList pList, ArgumentEvaluated argValue, LngObject meta, boolean[] filled, boolean lenient) {
        if (argValue.value instanceof Spread) {
            throw new ExecutionException("Named argument cannot be spread");
        } else {
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
                    ctx.defineTypeChecked(parameter.identifier(), argValue.value, calculateTypeNames(ctx, parameter.types()));
                    return;
                }
            }
            if (pList.meta() != null) {
                meta.setField(argValue.id.name(), argValue.value);
                return;
            }
            if (!lenient) {
                throw new ExecutionException("The parameter '%s' is not defined and there is no {meta} parameter", argValue.id.name());
            }
        }
    }

    /**
     * Add a positional parameter to the next non-named only free positional parameter.
     * <p>
     * if the value is a {@link Spread} object, then the values of the list are added to the arguments.
     *
     * @param ctx      the context to create the variable
     * @param pList    the descriptor of the parameters
     * @param argValue the argument values
     * @param rest     the rest object
     * @param filled   the array keeping track of which parameters had got value from the caller
     * @throws ExecutionException if there is no rest and there are too many positional parameters
     */
    private static void addPositionalParameter(LocalContext ctx,
                                               ParameterList pList,
                                               ArgumentEvaluated argValue,
                                               LngList rest,
                                               LngObject meta,
                                               boolean[] filled,
                                               boolean freeze) {
        if (argValue.value instanceof Spread(Object list)) {
            switch (list) {
                case null -> {
                }
                case HasFields it when !(list instanceof LngList) && !(list instanceof AsyncStreamHandler) -> {
                    for (final String name : it.fields()) {
                        final var value = it.getField(name);
                        addNamedParameter(ctx, pList, new ArgumentEvaluated(new Identifier(name), value), meta, filled, true);
                    }
                }
                case Iterable<?> it -> {
                    for (Object o : it) {
                        addPositionalParameter(ctx, pList, new ArgumentEvaluated(null, o), rest, meta, filled, freeze);
                    }
                }
                default -> throw new ExecutionException("You can only spread objects and lists, not '%s'", list);
            }
        } else {
            for (int index = 0; true; index++) {
                if (index >= pList.parameters().length) {
                    if (pList.rest() == null) {
                        throw new ExecutionException("Too many parameters, and there is no [rest] specified");
                    }
                    rest.array.add(argValue.value);
                    break;
                }
                if (pList.parameters()[index].type() != ParameterList.Parameter.Type.NAMED_ONLY && !filled[index]) {
                    final var id = pList.parameters()[index].identifier();
                    ctx.defineTypeChecked(id, argValue.value, calculateTypeNames(ctx, pList.parameters()[index].types()));
                    if (freeze) {
                        ctx.freeze(id);
                    }
                    filled[index] = true;
                    break;
                }
            }
        }
    }

    /**
     * Freeze the variable "this" and "cls" in the context.
     *
     * @param ctx the context in which we have to freeze "this"
     */
    public static void freezeThisAndCls(LocalContext ctx) {
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
    public static void freezeCls(LocalContext ctx) {
        if (ctx.contains("cls")) {
            ctx.freeze("cls");// better do not change 'this' inside methods
        }
    }

    /**
     * Get the method from the object. If it is a JavaObject, but the contained object has a TuriClass implementing
     * functionality, then get that functionality instead.
     *
     * @param context    the context to get access to the interpreter and, through that, to the registered TuriClasses
     * @param obj        the object for which we are searching the method
     * @param identifier the name of the method
     * @return the method object that can be a closure
     */
    protected static Object getMethod(LocalContext context, HasFields obj, String identifier) {
        return switch (obj) {
            case null -> null;
            case JavaObject jo -> {
                if (jo.object() == null) {
                    yield TuriNone.INSTANCE.getMethod(null, identifier);
                }
                final var turi = getTuriClass(context, jo);
                if (turi != null) {
                    yield turi.getMethod(jo.object(), identifier);
                }
                yield jo.getField(identifier);
            }
            default -> {
                final var method = obj.getField(identifier);
                if (method == null) {
                    yield obj.getField(".");
                } else {
                    yield method;
                }
            }
        };
    }

    /**
     * Returns the {@link TuriClass} associated with the class of the given {@link JavaObject}, searching its class hierarchy and interfaces if necessary.
     *
     * @param context the context used to resolve TuriClass associations
     * @param jo      the JavaObject whose class is used for lookup
     * @return the associated TuriClass, or null if none is found
     */
    public static TuriClass getTuriClass(LocalContext context, JavaObject jo) {
        var turi = context.globalContext.getTuriClass(jo.object().getClass());
        if (turi == null) {
            var papi = jo.object().getClass();
            while (papi != null) {
                turi = context.globalContext.getTuriClass(papi);
                if (turi != null) {
                    break;
                }
                for (final var face : papi.getInterfaces()) {
                    turi = context.globalContext.getTuriClass(face);
                    if (turi != null) {
                        break;
                    }
                }
                if (turi != null) {
                    break;
                }
                papi = papi.getSuperclass();
            }

        }
        return turi;
    }

    /**
     * Convert the {@code object} to a field access if it is an identifier referencing an object or class field in the
     * current context.
     * <p>
     * Helps to handle the method calls when there is no preceding "this" or "cls" in front of the method name.
     * <p>
     * If the {@code object} is an {@link Identifier} with a {@code name}, and the context contains a
     * {@code "this"} or {@code cls} reference, then the method checks whether the {@code this} object (assumed to be an
     * {@link LngObject}) has a field with the given name. If such a field exists, it returns a
     * {@link FieldAccess} expression referring to {@code this.name}.
     * <p>
     * If not, then it tries the same with the {@code cls} class object assuming it is {@code LngClass}.
     * <p>
     * Otherwise, it returns the original {@code object}.
     *
     * @param context the execution context used for evaluating "this" or "cls" and their local fields
     * @return the resulting Command object, either transformed into a FieldAccess Command or the original Command object
     */
    protected Command myFunctionObject(final LocalContext context) {
        final Command myObject;
        if (object instanceof Identifier id && (context.contains("this") || context.contains("cls"))) {
            final var thisObject = context.contains("this") ? context.get("this") : null;
            final var clsObject = context.contains("cls") ? context.get("cls") : null;
            if (thisObject instanceof LngObject lngObject && lngObject.context().containsLocal(id.name())) {
                myObject = new FieldAccess(new Identifier("this"), id.name(), false);
            } else if (clsObject instanceof LngClass lngClass && lngClass.context().containsLocal(id.name())) {
                myObject = new FieldAccess(new Identifier("cls"), id.name(), false);
            } else {
                myObject = object;
            }
        } else {
            myObject = object;
        }
        return myObject;
    }

    /**
     * Calculate the type names from the types.
     *
     * @param ctx   the current context to execute the expressions when a type is defined as an expression.
     * @param types the type names declared
     * @return the array of actual type names
     */
    public static String[] calculateTypeNames(final LocalContext ctx, TypeDeclaration[] types) {
        if (types != null) {
            return Arrays.stream(types).map(t -> t.calculateTypeName(ctx)).toArray(String[]::new);
        } else {
            return EMPTY_STRING_ARRAY;
        }
    }
}
