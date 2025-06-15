package ch.turic.memory;

import ch.turic.ExecutionException;
import ch.turic.commands.Closure;
import ch.turic.commands.Identifier;
import ch.turic.commands.Macro;
import ch.turic.commands.operators.Cast;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * An object that holds information about a variable
 */
public class Variable {
    public Variable(String name) {
        this.name = name;
    }

    /**
     * @param javaType    the declared types of the variable or null if there are no declared types
     * @param lngClass    the class object if the types is LngObject, otherwise null and ignored
     * @param declaration is the name used to define the type. It is used only for printouts.
     */
    public record Type(Class<?> javaType, LngClass lngClass, String declaration) {

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

    public void set(Object newValue) throws ExecutionException {
        if (!isOfTypes(newValue, types)) {
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
        this.value = newValue;
    }

    public static boolean isOfTypes(final Object value, Type[] types) {
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

    public static Type[] getTypes(final Context context, final String[] names) {
        return names == null ? NO_TYPE :
                Arrays.stream(names).map((s) -> getTypeFromName(context, s)).toArray(Type[]::new);
    }


    /**
     * Resolves a type name to a {@link Variable.Type} instance within the given context.
     * <p>
     * The method maps predefined types names (e.g., {@code "bool"}, {@code "str"}, {@code "num"}, etc.)
     * to their corresponding Java classes or internal representations. It also handles custom class names
     * either by prefixing with {@code "java."} to identify a Java class dynamically, or by looking up
     * user-defined classes in the provided {@link Context}.
     *
     * <p>Recognized built-in types:
     * <ul>
     * <li> {@code bool} boolean type
     * <li> {@code str} string
     * <li> {@code num} any numeric type, integer or float
     * <li> {@code float} float type
     * <li> {@code any} the variable can hold any value
     * <li> {@code obj} the variable can hols any object without restriction on the class of that object
     * <li> {@code lst} the variable has to be a list
     * <li> {@code que} the variable has to be a queue
     * <li> {@code task} the variable has to be an asynchronous task
     * <li> {@code err} the variable has to be an asynchronous task
     * <li> {@code cls} the variable has to be a class
     * <li> {@code fn} the variable value has to be a function of closure
     * <li> {@code macro} the variable value has to be a macro
     * <li> {@code none} the variable can hold the value {@code none}
     * <li> {@code some} the variable can hold any value, except {@code none}
     * </ul>
     * <p>
     * If the type name starts with {@code "java."}, the method tries to load the corresponding class
     * using reflection. If the type name is not one of the above and doesn't start with {@code "java."},
     * the method looks it up in the given context and expects it to be an instance of {@link LngClass}.
     *
     * @param context the current execution context in which user-defined classes are stored
     * @param name    the string representation of the types to resolve
     * @return a {@link Variable.Type} object representing the resolved types
     * @throws ExecutionException if the types cannot be found, is not defined in the context,
     *                            or is not a class when expected
     */
    public static Type getTypeFromName(Context context, String name) {
        return switch (name) {
            // snippet types
            case "bool" -> new Variable.Type(Boolean.class, null, name);
            // boolean type
            case "str" -> new Variable.Type(String.class, null, name);
            // string
            case "num" -> new Variable.Type(Long.class, null, name);
            // any numeric type, integer or float
            case "float" -> new Variable.Type(Double.class, null, name);
            // float type
            case "any" -> new Variable.Type(null, null, name);
            // the variable can hold any value
            case "obj" -> new Variable.Type(LngObject.class, null, name);
            // the variable can hols any object without restriction on the class of that object
            case "lst" -> new Variable.Type(LngList.class, null, name);
            // the variable has to be a list
            case "que" -> new Variable.Type(Channel.class, null, name);
            // the variable has to be a queue
            case "task" -> new Variable.Type(AsyncStreamHandler.class, null, name);
            // the variable has to be an asynchronous task
            case "err" -> new Variable.Type(LngException.class, null, name);
            // the variable has to be an asynchronous task
            case "cls" -> new Variable.Type(LngClass.class, null, name);
            // the variable has to be a class
            case "fn" -> new Variable.Type(Closure.class, null, name);
            // the variable value has to be a function of closure
            case "macro" -> new Variable.Type(Macro.class, null, name);
            // the variable value has to be a macro
            case "none" -> new Variable.Type(NoneType.class, null, name);
            // the variable can hold the value `none`
            case "some" -> new Variable.Type(SomeType.class, null, name);
            // the variable can hold any value, except `none`
            // end snippet
            default -> {
                if (name.startsWith("java.")) {
                    try {
                        yield new Variable.Type(Class.forName(name.substring(5)), null, name);
                    } catch (ClassNotFoundException e) {
                        throw new ExecutionException("Type '%s' could not be found.", name);
                    }
                }
                if (context == null) {
                    throw new RuntimeException("Null context, internal error.");
                }
                ExecutionException.when(!context.contains(name), "Type '%s' is not defined.", name);
                final var classObject = context.get(name);
                if (classObject instanceof LngClass lngClass) {
                    yield new Variable.Type(LngObject.class, lngClass, name);
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
            if (javaType == Double.class || javaType == Float.class) return Cast.isDouble(value);
            return value != null && javaType.isAssignableFrom(value.getClass());
        }
    }

    public Object get() {
        return value;
    }

}
