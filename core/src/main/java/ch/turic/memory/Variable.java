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
     * @param javaType the declared types of the variable or null if there are no declared types
     * @param lngClass the class object if the types is LngObject, otherwise null and ignored
     * @param declaration is the name used to define the type
     */
    public record Type(Class<?> javaType, LngClass lngClass, Identifier declaration) {
        @Override
        public String toString() {
            return declaration.name();
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

    public void set(Object newValue) {
        if (!isOfTypes(newValue, types)) {
            throw new ExecutionException(
                    "Cannot set variable '%s' to value '%s' as it does not fit any of the accepted type of the variable (%s)",
                    name,
                    Objects.requireNonNullElse(newValue,"none"),
                    Arrays.stream(types).map(Type::toString).collect(Collectors.joining("|")));
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
     *   <li>{@code "bool"} boolean type stored as a {@link Boolean}</li>
     *   <li>{@code "str"} is a string stored as {@link String}</li>
     *   <li>{@code "num"} a number stored as {@link Long}</li>
     *   <li>{@code "float"} a number stored as {@link Double}</li>
     *   <li>{@code "any"} any type, it is represented by the {@code null} value as a type</li>
     *   <li>{@code "obj"}, {@code "lst"} any object without defining the actual class {@link LngObject}</li>
     *   <li>{@code "cls"} a class type{@link LngClass}</li>
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
            case "bool" -> new Variable.Type(Boolean.class, null, new Identifier(name));
            // boolean type
            case "str" -> new Variable.Type(String.class, null, new Identifier(name));
            // string
            case "num" -> new Variable.Type(Long.class, null, new Identifier(name));
            // any numeric type, integer or float
            case "float" -> new Variable.Type(Double.class, null, new Identifier(name));
            // float type
            case "any" -> new Variable.Type(null, null, new Identifier(name));
            // the variable can hold any value
            case "obj" -> new Variable.Type(LngObject.class, null, new Identifier(name));
            // the variable can hols any object without restriction on the class of that object
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
            // end snippet
            default -> {
                if (name.startsWith("java.")) {
                    try {
                        yield new Variable.Type(Class.forName(name.substring(5)), null, new Identifier(name));
                    } catch (ClassNotFoundException e) {
                        throw new ExecutionException("Type '%s' could not be found.", name);
                    }
                }
                if( context == null ){
                    throw new RuntimeException("Null context, internal error.");
                }
                ExecutionException.when(!context.contains(name), "Type '%s' is not defined.", name);
                final var classObject = context.get(name);
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

            return (javaType == NoneType.class && value == null) ||
                    (javaType == Double.class || javaType == Float.class && Cast.isDouble(value)) ||
                    (value != null && javaType.isAssignableFrom(value.getClass()));
        }
    }

    public Object get() {
        return value;
    }

}
