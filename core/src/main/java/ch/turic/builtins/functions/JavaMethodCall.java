package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;

import java.lang.reflect.InvocationTargetException;

/**
 * Represents a function in the Turi language to invoke Java methods dynamically.
 * This class implements the TuriFunction interface and facilitates calling specified
 * Java methods on classes or objects, handling method overloading, variable arguments,
 * and type conversions as necessary.
 */
public class JavaMethodCall implements TuriFunction {
    @Override
    public String name() {
        return "java_call";
    }

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        final var args = FunUtils.args(name(), arguments, Object.class, String.class, Object[].class);
        final var methodName = args.at(1).as(String.class);

        final var tuple = getKlassAndObject(args);
        final var klass = tuple.klass();
        final var object = tuple.object();

        for (final var method : klass.getMethods()) {
            if (!method.getName().equals(methodName) || method.isSynthetic()) {
                continue;
            }
            if (method.isVarArgs()) {
                if (method.getParameterCount() <= args.N - 2) {
                    continue;
                }
            } else {
                if (method.getParameterCount() != args.N - 2) {
                    continue;
                }
            }
            int i = 2;
            if (method.isVarArgs()) {
                for (int j = 0; j < method.getParameterTypes().length - 1; j++, i++) {
                    final var pType = method.getParameterTypes()[j];
                    if (!pType.isAssignableFrom(args.at(i).type)) {
                        break;
                    }
                }
                final var lastPType = method.getParameterTypes()[method.getParameterTypes().length - 1].arrayType();
                while (i < args.N) {
                    if (!lastPType.isAssignableFrom(args.at(i).type)) {
                        break;
                    }
                    i++;
                }

            } else {
                for (final var pType : method.getParameterTypes()) {
                    if (!isAssignable(pType, args.at(i).get())) {
                        break;
                    }
                    i++;
                }
            }
            if (i == args.N) {
                try {
                    if (method.isVarArgs()) {
                        final Object[] javaArgs = new Object[method.getParameterCount()];
                        int k = 2, h = 0;
                        while (h < method.getParameterCount() - 1) {
                            javaArgs[h++] = args.at(k++).get();
                        }
                        Class<?> varargComponentType = method.getParameterTypes()[method.getParameterCount() - 1].getComponentType();
                        Object varargs = java.lang.reflect.Array.newInstance(varargComponentType, args.N - k);
                        for (int v = 0; k < args.N; k++, v++) {
                            java.lang.reflect.Array.set(varargs, v, args.at(k).get());
                        }
                        javaArgs[h] = varargs;
                        return method.invoke(object, javaArgs);
                    } else {
                        final Object[] ajavArgs = args.tail(2);
                        return method.invoke(object, ajavArgs);
                    }
                }catch( InvocationTargetException ite){
                    Throwable cause = ite.getCause();
                    while(cause.getCause() != null){
                        cause = cause.getCause();
                    }
                    throw new ExecutionException(cause,"Exception while executing method call '"+methodName+"'");
                }
                catch (IllegalAccessException  e) {
                    throw new ExecutionException("Cannot invoke method '" + methodName + "'.", e);
                }
            }
        }
        throw new ExecutionException("Cannot find method '" + methodName + "'.");
    }

    /**
     * Determines if the given argument is assignable to the specified parameter type.
     *
     * @param parameterType the target class type to check against
     * @param arg the object to check for assignability
     * @return true if the argument can be assigned to the parameter type, false otherwise
     */
    private static boolean isAssignable(Class<?> parameterType, Object arg) {
        if (arg == null) return !parameterType.isPrimitive(); // null can only go into non-primitives
        Class<?> argClass = arg.getClass();
        if (parameterType.isPrimitive()) {
            return switch (parameterType.getName()) {
                case "boolean" -> argClass == Boolean.class;
                case "byte"    -> argClass == Byte.class;
                case "char"    -> argClass == Character.class;
                case "short"   -> argClass == Short.class;
                case "int"     -> argClass == Integer.class;
                case "long"    -> argClass == Long.class;
                case "float"   -> argClass == Float.class;
                case "double"  -> argClass == Double.class;
                default        -> false;
            };
        } else {
            return parameterType.isAssignableFrom(argClass);
        }
    }

    /**
     * Processes the provided arguments to determine the corresponding class and object.
     * If the first argument is a String, it is treated as a class name, and the method
     * attempts to load the corresponding class. If the first argument is an object, which is not a String, the
     * class of the object is determined, and the object itself is returned.
     *
     * @param args the arguments of the call to the function
     * @return a record containing the determined class and object. If the first
     *         argument is a String (class name), the returned object is null.
     * @throws ExecutionException if the class corresponding to the provided
     *                            class name cannot be loaded.
     */
    static Tuple getKlassAndObject(FunUtils.ArgumentsHolder args) throws ExecutionException {
        final Class<?> klass;
        final Object object;
        if ( args.at(0).is_a(String.class)) {// if it is a string then it is a class name, we do not call string methods from Turicum
            final var className = args.at(0).as(String.class);
            try {
                klass = Class.forName(className);
                object = null;
            } catch (ClassNotFoundException ex) {
                throw new ExecutionException("Cannot find class '" + className + "'.", ex);
            }
        } else {
            object = args.at(0).get();
            klass = object.getClass();
        }
        return new Tuple(klass, object);
    }

    record Tuple(Class<?> klass, Object object) {
    }

}
