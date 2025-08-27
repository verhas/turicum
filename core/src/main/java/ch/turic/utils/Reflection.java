package ch.turic.utils;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiPredicate;

public class Reflection {

    /**
     * Retrieves a {@link Method} object for a given object's class that matches the specified
     * method name and argument types. The argument types are inferred from the provided arguments,
     * including handling of null values.
     *
     * @param object     the object whose class will be searched for the method, must not be null
     * @param methodName the name of the method to retrieve must not be null
     * @param args       an array of objects representing the arguments to the method; the types
     *                   of these objects are used to infer the method's parameter types
     * @return a {@link Method} object that matches the specified name and inferred parameter types,
     * or null if no matching method is found
     */
    public static Method getMethodForArgs(final Object object, final String methodName, final Object[] args) {
        return getMethod(object, methodName, Arrays.stream(args).map(o -> o == null ? null : o.getClass()).toArray(Class[]::new));
    }

    /**
     * Retrieves a static method from the specified class matching the given method name
     * and parameter argument types inferred from the provided arguments.
     *
     * @param klass      the class containing the static method
     * @param methodName the name of the static method to retrieve
     * @param args       the arguments used to infer the parameter types of the method
     * @return the matching static method, or null if no matching method is found
     */
    public static Method getStaticMethodForArgs(final Class<?> klass, final String methodName, final Object[] args) {
        return getStaticMethod(klass, methodName, Arrays.stream(args).map(o -> o == null ? null : o.getClass()).toArray(Class[]::new));
    }

    /**
     * Retrieves a static method from the specified class or its interfaces based on the provided method name
     * and parameter types. If the method is found and accessible, it returns the Method instance.
     *
     * @param klass      the Class object from which the static method is to be retrieved; must not be null
     * @param methodName the name of the method to be retrieved; must not be null
     * @param args       the array of parameter types for the method; must not be null
     * @return the Method object representing the static method if found and accessible; otherwise, null
     */
    public static Method getStaticMethod(final Class<?> klass, final String methodName, final Class<?>[] args) {
        Objects.requireNonNull(klass, "klass must not be null");
        Objects.requireNonNull(methodName, "methodName must not be null");
        Objects.requireNonNull(args, "args must not be null");
        final var method = getMethodFromClass(klass, methodName, args);
        if (method == null) {
            return null;
        }
        try {
            method.setAccessible(true);
            if (method.canAccess(null)) {
                return method;
            }
        } catch (InaccessibleObjectException ignored) {
        }
        for (final var interfac: collectInterfaces(klass)) {
            final var interfaceMethod = getMethodFromClass(interfac, methodName, args);
            if (interfaceMethod != null) {
                try {
                    interfaceMethod.setAccessible(true);
                    if (interfaceMethod.canAccess(null)) {
                        return interfaceMethod;
                    }
                } catch (InaccessibleObjectException ignored) {
                }
            }
        }
        return null;
    }

    /**
     * Invokes the specified method on the given target object with the provided arguments.
     * Handles methods with variable arguments (varargs) by appropriately packing the varargs
     * into an array.
     *
     * @param method the method to be invoked
     * @param target the object on which the specified method is to be invoked. It can be null for static methods
     * @param args   the arguments to pass to the method. For vararg methods, this should
     *               include all arguments, with the variable arguments at the end
     * @return the result of invoking the method
     * @throws Exception if an exception occurs during invocation, such as an {@code IllegalAccessException},
     *                   {@code IllegalArgumentException}, or any exception thrown by the called method
     */
    public static Object invoke(Method method, Object target, Object[] args) throws Exception {
        Objects.requireNonNull(method, "method must not be null");
        if (method.isVarArgs()) {
            Class<?>[] paramTypes = method.getParameterTypes();
            int normalCount = paramTypes.length - 1; // all before the vararg

            Object[] packed = new Object[paramTypes.length];

            // Copy fixed arguments
            if (normalCount >= 0) System.arraycopy(args, 0, packed, 0, normalCount);

            // Pack the varargs tail
            Class<?> varargType = paramTypes[normalCount].getComponentType();
            int varargCount = args.length - normalCount;
            Object varargsArray = Array.newInstance(varargType, varargCount);

            for (int i = 0; i < varargCount; i++) {
                Array.set(varargsArray, i, args[normalCount + i]);
            }

            packed[normalCount] = varargsArray;

            return method.invoke(target, packed);
        } else {
            // Normal case, pass through
            return method.invoke(target, args);
        }
    }

    public static <T> T newInstance(Constructor<?> constructor, Object[] args) throws Exception {
        Objects.requireNonNull(constructor, "method must not be null");
        if (constructor.isVarArgs()) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            int normalCount = paramTypes.length - 1; // all before the vararg

            Object[] packed = new Object[paramTypes.length];

            // Copy fixed arguments
            if (normalCount >= 0) System.arraycopy(args, 0, packed, 0, normalCount);

            // Pack the varargs tail
            Class<?> varargType = paramTypes[normalCount].getComponentType();
            int varargCount = args.length - normalCount;
            Object varargsArray = Array.newInstance(varargType, varargCount);

            for (int i = 0; i < varargCount; i++) {
                Array.set(varargsArray, i, args[normalCount + i]);
            }

            packed[normalCount] = varargsArray;

            return (T)constructor.newInstance(packed);
        } else {
            // Normal case, pass through
            return (T)constructor.newInstance(args);
        }
    }

    /**
     * Retrieves a {@link Method} object from a given object's class or its implemented interfaces
     * that matches the indicated method name and parameter types. The method is made accessible
     * if it is found and can be accessed by the provided object.
     *
     * @param object     the object whose class or interfaces will be searched for the method
     *                   must not be null
     * @param methodName the name of the method to retrieve must not be null
     * @param args       an array of {@link Class} objects representing the parameter types
     *                   of the method
     * @return a {@link Method} object matching the provided name and parameter types,
     * or null if no matching method is found, or it cannot be accessed
     */
    public static Method getMethod(final Object object, final String methodName, final Class<?>[] args) {
        Objects.requireNonNull(object, "object must not be null");
        Objects.requireNonNull(methodName, "methodName must not be null");
        Objects.requireNonNull(args, "args must not be null");
        final var klass = object.getClass();
        final var method = getMethodFromClass(klass, methodName, args);
        if (method == null) {
            return null;
        }
        try {
            method.setAccessible(true);
            if (method.canAccess(object)) {
                return method;
            }
        } catch (InaccessibleObjectException ignored) {
        }
        for (final var interfac : collectInterfaces(klass)) {
            final var interfaceMethod = getMethodFromClass(interfac, methodName, args);
            if (interfaceMethod != null) {
                try {
                    interfaceMethod.setAccessible(true);
                    if (interfaceMethod.canAccess(object)) {
                        return interfaceMethod;
                    }
                } catch (InaccessibleObjectException ignored) {
                }
            }
        }
        return null;
    }

    public static Constructor<?> getConstructorForArgs(final Class<?> klass, final Object[] args) {
        return getConstructor(klass, Arrays.stream(args).map(o -> o == null ? null : o.getClass()).toArray(Class[]::new));
    }

    public static Constructor<?> getConstructor(final Class<?> klass, final Class<?>[] args) {
        final var constructors = preselectConstructors(klass.getConstructors(), args);
        final var executable = selectFitting(args, constructors.stream().map(c -> (Executable) c).toList());
        if (executable == null) {
            return null;
        }
        if (executable instanceof Constructor<?> constructor) {
            return constructor;
        } else {
            throw new IllegalStateException("getConstructors returned " + executable.getClass().getName() + " instead of Constructor");
        }
    }

    /**
     * Collects all interfaces implemented by a given class and its superclasses.
     *
     * @param klass the {@link Class} object whose interfaces and ancestor interfaces
     *              are to be collected, must not be null
     * @return a {@link Set} containing all interfaces implemented by the specified class
     * and its superclasses
     */
    private static Set<Class<?>> collectInterfaces(Class<?> klass) {
        final var interfaces = new HashSet<Class<?>>();
        collectInterfaces(klass, interfaces);
        return interfaces;
    }

    /**
     * Recursively collects all interfaces implemented by the specified class and its superclasses.
     *
     * @param klass  the {@link Class} object to inspect for implemented interfaces, must not be null
     * @param interfaces a {@link Set} to which the collected interfaces are added, must not be null
     */
    private static void collectInterfaces(Class<?> klass, Set<Class<?>> interfaces) {
        interfaces.addAll(Arrays.stream(klass.getInterfaces()).toList());
        final var parent = klass.getSuperclass();
        if (parent != null) {
            collectInterfaces(parent, interfaces);
        }
    }

    /**
     * Retrieves a method from the specified class that matches the given method name and parameter types.
     * It first searches through the class's public methods and then its declared methods.
     *
     * @param klass      the {@link Class} object from which the method is to be retrieved
     * @param methodName the name of the method to search for
     * @param args       an array of {@link Class} objects representing the parameter types of the method
     * @return the {@link Method} object that matches the specified criteria, or null if no matching method is found
     */
    private static Method getMethodFromClass(final Class<?> klass, final String methodName, final Class<?>[] args) {
        final List<Executable> methods = preselectMethods(klass.getMethods(), methodName, args).stream().map(m -> (Executable) m).toList();
        final var method = selectFitting(args, methods);
        if (method != null) return (Method) method;
        final var methodsDeclared = preselectMethods(klass.getDeclaredMethods(), methodName, args).stream().map(m -> (Executable) m).toList();
        return (Method) selectFitting(args, methodsDeclared);
    }

    /**
     * Filters the provided array of methods to select those that match the given method name
     * and have a parameter count that matches the expected argument length.
     *
     * @param methods    an array of {@link Method} objects to filter
     * @param methodName the name of the method to match
     * @param args       an array of {@link Class} objects representing the expected parameter types
     * @return a list of {@link Method} objects that match the specified name and parameter count
     */
    private static List<Method> preselectMethods(Method[] methods, final String methodName, final Class<?>[] args) {
        return Arrays.stream(methods).filter(m -> nameMatches(methodName, m)).filter(m -> argsNumberMatches(args.length, m)).toList();
    }

    /**
     * Filters and preselects constructors based on the number of arguments they accept.
     *
     * @param constructors the array of constructors to evaluate
     * @param args         the array of argument types to match the constructors against
     * @return a list of constructors whose parameter count matches the length of the provided arguments
     */
    private static List<Constructor<?>> preselectConstructors(Constructor<?>[] constructors, final Class<?>[] args) {
        return Arrays.stream(constructors).filter(m -> argsNumberMatches(args.length, m)).toList();
    }

    /**
     * Selects the most fitting executable (method or constructor) from a list of methods based on how well
     * their parameter types match the provided argument types.
     * <p>
     * The method prioritizes exact matches, followed by matches that allow for boxing, and finally, general
     * compatibility matches.
     *
     * @param args        an array of {@link Class} objects representing the argument types
     *                    to match against the methods' parameter types
     * @param executables a list of {@link Method} objects to evaluate and classify based on
     *                    their compatibility with the provided argument types
     * @return the most fitting {@link Method} object based on the matching criteria,
     * or null if no suitable method is found
     */
    private static Executable selectFitting(Class<?>[] args, List<Executable> executables) {
        return executables.stream()
                .filter(method -> fits(method, args, Reflection::acceptsSame))
                .findFirst()
                .orElseGet(() ->
                        executables.stream().filter(method -> fits(method, args, Reflection::acceptsBoxing))
                                .findFirst()
                                .orElseGet(() ->
                                        executables.stream().filter(method -> fits(method, args, Reflection::accepts))
                                                .findFirst()
                                                .orElse(null)
                                )
                );
    }

    /**
     * Determines whether a given method's parameter types match the provided argument types
     * based on a specified predicate for compatibility. The method can handle both varargs
     * and non-varargs method signatures.
     *
     * @param method    the {@link Method} object whose parameter types are to be checked
     * @param args      an array of {@link Class} objects representing the argument types
     *                  to validate against the method's parameters
     * @param predicate a {@link BiPredicate} used to test compatibility between each parameter
     *                  type and the corresponding argument type
     * @return true if the method's parameter types match the provided argument types
     * based on the compatibility defined by the predicate; false otherwise
     */
    private static boolean fits(Executable method, Class<?>[] args, BiPredicate<Class<?>, Class<?>> predicate) {
        return method.isVarArgs() ? varargFit(args, method, predicate) : nonVarargFit(args, method, predicate);
    }

    /**
     * Checks whether the number of arguments matches the parameter count of the given method.
     * For varargs methods, it ensures the number of arguments is at least equal to the fixed parameter count.
     *
     * @param n      the number of arguments to check against the method's parameter count
     * @param method the {@link Method} object whose parameter count will be checked
     * @return true if the number of arguments matches the method's parameter count, or if it satisfies
     * the requirements for a varargs method; false otherwise
     */
    private static boolean argsNumberMatches(final int n, final Executable method) {
        if (method.isVarArgs()) {
            // at least the fixed parameters are provided
            return method.getParameterCount() - 1 <= n;
        } else {
            return method.getParameterCount() == n;
        }
    }

    /**
     * Checks whether the provided method's name matches the given methodName and ensures
     * that the method is not synthetic.
     *
     * @param methodName the name of the method to compare
     * @param method     the {@link Method} object whose name is to be verified
     * @return true if the method's name matches the specified methodName and the method is not synthetic, false otherwise
     */
    private static boolean nameMatches(String methodName, Method method) {
        return method.getName().equals(methodName) && !method.isSynthetic();
    }


    private static boolean varargFit(Class<?>[] args, Executable method, BiPredicate<Class<?>, Class<?>> predicate) {
        int j = 0;
        for (; j < method.getParameterTypes().length - 1; j++) {
            final var pType = method.getParameterTypes()[j];
            if (!predicate.test(pType, args[j])) {
                return false;
            }
        }
        final var lastPType = method.getParameterTypes()[method.getParameterTypes().length - 1].getComponentType();
        for (; j < args.length; j++) {
            if (!predicate.test(lastPType, args[j])) {
                return false;
            }
        }
        return true;
    }

    private static boolean nonVarargFit(Class<?>[] args, Executable method, BiPredicate<Class<?>, Class<?>> predicate) {
        int i = 0;
        for (final var pType : method.getParameterTypes()) {
            if (!predicate.test(pType, args[i])) {
                return false;
            }
            i++;
        }
        return true;
    }

    /**
     * Determines if the given argument type is assignable to the specified parameter type,
     * including support for primitive widening conversions.
     *
     * @param parameterType the target class type to check against
     * @param argType       the argument type to check for assignability
     * @return true if the argument can be assigned to the parameter type, false otherwise
     */
    private static boolean accepts(Class<?> parameterType, Class<?> argType) {
        if (parameterType == null) return !argType.isPrimitive(); // null can only go into non-primitives

        if (parameterType.isPrimitive()) {
            return acceptsPrimitive(parameterType, argType);
        } else {
            return parameterType.isAssignableFrom(argType);
        }
    }

    private static boolean acceptsSame(Class<?> parameterType, Class<?> argType) {
        return parameterType == argType;
    }

    /**
     * Determines if a given parameter type can accept an argument type, considering boxing and unboxing
     * of primitive types.
     *
     * @param parameterType the expected parameter type, which may be a wrapper type for primitives
     *                      If it is null, it means that the actual value of the argument is null, the class
     *                      cannot be determined. In this case, all classes are okay except primitive types
     *                      that will cause NPE if you try to pass a null.
     * @param argType       the actual argument type provided
     * @return true if the parameter type accepts the argument type, including scenarios where boxing
     * or unboxing applies; false otherwise
     */
    private static boolean acceptsBoxing(Class<?> parameterType, Class<?> argType) {
        if (parameterType == null) return !argType.isPrimitive(); // null can only go into non-primitives
        if (argType.isPrimitive()) {
            return switch (argType.getName()) {
                case "boolean" -> parameterType == Boolean.class;
                case "byte" -> parameterType == Byte.class;
                case "char" -> parameterType == Character.class;
                case "short" -> parameterType == Short.class;
                case "int" -> parameterType == Integer.class;
                case "long" -> parameterType == Long.class;
                case "float" -> parameterType == Float.class;
                case "double" -> parameterType == Double.class;
                default -> false;
            };
        } else {
            return parameterType.isAssignableFrom(argType);
        }
    }

    /**
     * Checks if an argument type can be assigned to a primitive parameter type,
     * including widening primitive conversions as defined by JLS §5.1.2.
     *
     * @param primitiveParam the primitive parameter type
     * @param argumentType   the argument type (primitive wrapper or primitive)
     * @return true if the assignment is valid, false otherwise
     */
    private static boolean acceptsPrimitive(Class<?> primitiveParam, Class<?> argumentType) {
        // Handle wrapper to primitive conversions first
        Class<?> argPrimitive = unwrapIfWrapper(argumentType);
        if (argPrimitive == null) return false; // argumentType is not a primitive or wrapper

        // Exact match
        if (primitiveParam == argPrimitive) return true;

        // Widening primitive conversions (JLS §5.1.2)
        return switch (primitiveParam.getName()) {
            case "byte" -> false; // 'byte' accepts only 'byte'

            case "short" -> argPrimitive == byte.class; // 'short' accepts byte

            case "int" -> argPrimitive == byte.class ||
                    argPrimitive == short.class ||
                    argPrimitive == char.class; // 'int' accepts byte, 'short', char

            case "long" -> argPrimitive == byte.class ||
                    argPrimitive == short.class ||
                    argPrimitive == char.class ||
                    argPrimitive == int.class; // 'long' accepts 'byte', 'short', 'char', 'int'

            case "float" -> argPrimitive == byte.class ||
                    argPrimitive == short.class ||
                    argPrimitive == char.class ||
                    argPrimitive == int.class ||
                    argPrimitive == long.class; // 'float' accepts 'byte', 'short', 'char', 'int', 'long'

            case "double" -> argPrimitive == byte.class ||
                    argPrimitive == short.class ||
                    argPrimitive == char.class ||
                    argPrimitive == int.class ||
                    argPrimitive == long.class ||
                    argPrimitive == float.class; // 'double' accepts byte, short, char, int, long, float

            case "char" -> false; // char accepts only char
            case "boolean" -> false; // boolean accepts only boolean

            default -> false;
        };
    }

    /**
     * Converts a wrapper class to its corresponding primitive Java class, or returns the class
     * itself if it's already primitive.
     *
     * @param klass the class to unwrap
     * @return the primitive Java class, or null if the input is neither primitive nor a wrapper
     */
    private static Class<?> unwrapIfWrapper(Class<?> klass) {
        if (klass.isPrimitive()) return klass;

        return switch (klass.getName()) {
            case "java.lang.Boolean" -> boolean.class;
            case "java.lang.Byte" -> byte.class;
            case "java.lang.Character" -> char.class;
            case "java.lang.Short" -> short.class;
            case "java.lang.Integer" -> int.class;
            case "java.lang.Long" -> long.class;
            case "java.lang.Float" -> float.class;
            case "java.lang.Double" -> double.class;
            default -> null; // Not a wrapper type
        };
    }
}
