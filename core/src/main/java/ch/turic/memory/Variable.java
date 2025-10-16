package ch.turic.memory;

import ch.turic.exceptions.ExecutionException;
import ch.turic.commands.Closure;
import ch.turic.commands.Identifier;
import ch.turic.commands.Macro;
import ch.turic.commands.operators.Cast;
import ch.turic.utils.JdkTypePredicate;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * An object that holds information about a variable
 */
public class Variable {
    /**
     * Create a new variable for a name without defining the value or the different types it can be.
     *
     * @param name the name of the variable
     */
    public Variable(String name) {
        this.name = name;
    }

    /**
     * A variable can have one or more types. These types are checked and restrict the value of the variable.
     * Assigning a value to a variable that does not fit any of the types is an error.
     * This record describes one type.
     *
     * @param javaType    the declared type of the variable, or null if there are no declared types
     * @param lngClass    the class object if the type is LngObject, otherwise null and ignored
     * @param declaration is the name used to define the type. It is used only for logs, debug, and printouts.
     */
    public record Type(Class<?> javaType, LngClass lngClass, String declaration) {
        public Type(Class<?> javaType, LngClass lngClass, Identifier id) {
            this(javaType, lngClass, id.name());
        }

        @Override
        public String toString() {
            return declaration;
        }

        @Override
        public int hashCode() {
            return Objects.hash(javaType, lngClass);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Type type = (Type) obj;
            return Objects.equals(javaType, type.javaType) &&
                    Objects.equals(lngClass, type.lngClass);
        }

    }

    public final String name;
    // the current value of the variable
    Object value;
    Type[] types;

    /**
     * Set the value of the variable. Setting also checks that the value is assignable to this variable.
     * It is assignable if there is a type for this variable that is okay with this value.
     *
     * @param newValue the value to be assigned to the variable
     * @throws ExecutionException if the value does not fit any of the types.
     */
    public void set(Object newValue) throws ExecutionException {
        if (isOfTypes(newValue)) {
            this.value = newValue;
        } else {
            if (types.length == 1) {
                throw new ExecutionException(
                        "Cannot set variable '%s' to value '%s' because it does not fit the declared type %s",
                        name,
                        Objects.requireNonNullElse(newValue, "none"),
                        types[0].toString());
            } else {
                throw new ExecutionException(
                        "Cannot set variable '%s' to value '%s' because it does not fit any of the declared types of the variable (%s)",
                        name,
                        Objects.requireNonNullElse(newValue, "none"),
                        Arrays.stream(types).map(Type::toString).collect(Collectors.joining("|")));
            }
        }
    }

    /**
     * Checks that the value fits any of the types.
     *
     * @param value the value to be checked.
     * @return {@code true} if the value fits any of the types of the variable.
     */
    private boolean isOfTypes(final Object value) {
        if (types == null || types.length == 0) {
            return true;
        } else {
            for (final var type : types) {
                if (isFit(value, type.javaType(), type.lngClass())) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final Type[] NO_TYPE = new Type[0];

    public static Type[] getTypes(final LocalContext context, final String[] names) {
        return names == null ? NO_TYPE :
                Arrays.stream(names).map((s) -> getTypeFromName(context, s)).toArray(Type[]::new);
    }


    /**
     * Resolves a type name to a {@link Variable.Type} instance within the given context.
     * <p>
     * The method maps predefined types names (e.g., {@code "bool"}, {@code "str"}, {@code "num"}, etc.)
     * to their corresponding Java classes or internal representations. It also handles custom class names
     * either by prefixing with {@code "java."} to identify a Java class dynamically, or by looking up
     * user-defined classes in the provided {@link LocalContext}.
     *
     * <p>Recognized built-in types:
     * <ul>
     * <li> {@code bool} boolean type
     * <li> {@code str} string
     * <li> {@code num} any numeric type, integer, or float
     * <li> {@code int} any integer type
     * <li> {@code float} float type
     * <li> {@code any} the variable can hold any value
     * <li> {@code obj} the variable can hold any object without restriction on the class of that object
     * <li> {@code lst} the variable has to be a list
     * <li> {@code que} the variable has to be a queue
     * <li> {@code task} the variable has to be an asynchronous task
     * <li> {@code err} the variable has to be an asynchronous task
     * <li> {@code cls} the variable has to be a class
     * <li> {@code fn} the variable value has to be a function or closure
     * <li> {@code macro} the variable value has to be a macro
     * <li> {@code none} the variable can hold the value {@code none}
     * <li> {@code some} the variable can hold any value, except {@code none}
     * </ul>
     * <p>
     * If the type name starts with {@code "java."}, the method tries to load the corresponding class
     * using reflection. If the type name is not one of the above and doesn't start with {@code "java."},
     * the method looks it up in the given context and expects it to be an instance of {@link LngClass}.
     *
     * @param ctx  the current execution context in which user-defined classes are stored
     * @param name the string representation of the types to resolve
     * @return a {@link Variable.Type} object representing the resolved types
     * @throws ExecutionException if the types cannot be found, is not defined in the context,
     *                            or is not a class when expected
     */
    public static Type getTypeFromName(LocalContext ctx, String name) {
        return switch (name) {
            // snippet types
            case "bool" -> new Variable.Type(Boolean.class, null, new Identifier(name));
            // boolean type
            case "str" -> new Variable.Type(String.class, null, new Identifier(name));
            // string
            case "num" -> new Variable.Type(Number.class, null, new Identifier(name));
            // any numeric type, integer or float
            case "int" -> new Variable.Type(Long.class, null, new Identifier(name));
            // any integer type
            case "float" -> new Variable.Type(Double.class, null, new Identifier(name));
            // float type
            case "any" -> new Variable.Type(null, null, new Identifier(name));
            // the variable can hold any value
            case "obj" -> new Variable.Type(LngObject.class, null, new Identifier(name));
            // the variable can hold any object without restriction on the class of that object
            case "lst" -> new Variable.Type(LngList.class, null, new Identifier(name));
            // the variable has to be a list
            case "que" -> new Variable.Type(Channel.class, null, new Identifier(name));
            // the variable has to be a queue
            case "task" -> new Variable.Type(AsyncStreamHandler.class, null, new Identifier(name));
            // the variable has to be an asynchronous task
            case "err" -> new Variable.Type(LngException.class, null, new Identifier(name));
            // the variable has to be an asynchronous task
            case "cls" -> new Variable.Type(LngClass.class, null, new Identifier(name));
            // the variable has to be a class
            case "fn" -> new Variable.Type(Closure.class, null, new Identifier(name));
            // the variable value has to be a function of closure
            case "macro" -> new Variable.Type(Macro.class, null, new Identifier(name));
            // the variable value has to be a macro
            case "none" -> new Variable.Type(NoneType.class, null, new Identifier(name));
            // the variable can hold the value `none`
            case "some" -> new Variable.Type(SomeType.class, null, new Identifier(name));
            // the variable can hold any value, except `none`
            // end snippet
            default -> {
                if (name.startsWith("java.")) {
                    final String klassName;
                    if (JdkTypePredicate.INSTANCE.test(name)) {
                        klassName = name;
                    } else {
                        klassName = name.substring(5);
                    }
                    try {
                        final var klass = ctx.globalContext.classLoader.loadClass(klassName);
                        yield new Variable.Type(klass, null, new Identifier(name));
                    } catch (ClassNotFoundException e) {
                        throw new ExecutionException("Type '%s' could not be found.", name);
                    }
                }
                if (ctx == null) {
                    throw new RuntimeException("Null context, internal error.");
                }
                ExecutionException.when(!ctx.contains(name), "Type '%s' is not defined.", name);
                final var classObject = ctx.get(name);
                if (classObject instanceof LngClass lngClass) {
                    yield new Variable.Type(LngObject.class, lngClass, new Identifier(name));
                } else {
                    throw new ExecutionException("Type '%s' is not a class.", name);
                }
            }
        };
    }

    public static boolean isFit(Object value, Class<?> javaType, LngClass lngClass) {
        if (javaType == null) {
            return true;
        }
        if (javaType == LngObject.class) {
            if (value instanceof LngObject lngObject) {
                return lngObject.instanceOf(lngClass);
            } else {
                return false;
            }
        } else {
            if (javaType == NoneType.class) return value == null;
            if (javaType == SomeType.class) return value != null;
            if (javaType == Double.class) return Cast.isDouble(value);
            if (javaType == Long.class) return Cast.isLong(value);
            return value != null && javaType.isAssignableFrom(value.getClass());
        }
    }

    public Object get() {
        return value;
    }

}
