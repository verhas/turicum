package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/**
 * A function that calls a Java method.
 */
public class JavaMethodCall implements TuriFunction {
    @Override
    public String name() {
        return "java_call";
    }

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        FunUtils.twoArgs(name(), arguments);
        if (!(arguments[1] instanceof String methodName)) {
            throw new ExecutionException("Function %s needs a method name as a second argument.", name());
        }
        final var klassAndObject = getKlassAndObject(arguments);
        final var klass = klassAndObject.klass();
        final var object = klassAndObject.object();
        for (final var method : klass.getMethods()) {
            if (!method.getName().equals(methodName) || method.isSynthetic()) {
                continue;
            }
            if (method.isVarArgs()) {
                if (method.getParameterCount() <= arguments.length - 2) {
                    continue;
                }
            } else {
                if (method.getParameterCount() != arguments.length - 2) {
                    continue;
                }
            }
            int i = 2;
            if (method.isVarArgs()) {
                for (int j = 0; j < method.getParameterTypes().length - 1; j++, i++) {
                    final var pType = method.getParameterTypes()[j];
                    if (!pType.isAssignableFrom(arguments[i].getClass())) {
                        break;
                    }
                }
                final var lastPType = method.getParameterTypes()[method.getParameterTypes().length - 1].arrayType();
                while (i < arguments.length) {
                    if (!lastPType.isAssignableFrom(arguments[i].getClass())) {
                        break;
                    }
                    i++;
                }

            } else {
                for (final var pType : method.getParameterTypes()) {
                    if (!pType.isAssignableFrom(arguments[i].getClass())) {
                        break;
                    }
                    i++;
                }
            }
            if (i == arguments.length) {
                try {
                    if (method.isVarArgs()) {
                        final Object[] args = new Object[method.getParameterCount()];
                        int k = 2, h = 0;
                        while (h < method.getParameterCount() - 1) {
                            args[h++] = arguments[k++];
                        }
                        Class<?> varargComponentType = method.getParameterTypes()[method.getParameterCount() - 1].getComponentType();
                        Object varargs = java.lang.reflect.Array.newInstance(varargComponentType, arguments.length - k);
                        for (int v = 0; k < arguments.length; k++, v++) {
                            java.lang.reflect.Array.set(varargs, v, arguments[k]);
                        }
                        args[h] = varargs;
                        return method.invoke(object, args);
                    } else {
                        final Object[] args = Arrays.copyOfRange(arguments, 2, arguments.length);
                        return method.invoke(object, args);
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new ExecutionException("Cannot invoke method '" + methodName + "'.", e);
                }
            }
        }
        throw new ExecutionException("Cannot find method '" + methodName + "'.");
    }

    static KlassAndObject getKlassAndObject(Object[] arguments) {
        final Class<?> klass;
        final Object object;
        if (arguments[0] instanceof String className) {// if it is a string then it is a class name, we do not call string methods from Turicum
            try {
                klass = Class.forName(className);
                object = null;
            } catch (ClassNotFoundException ex) {
                throw new ExecutionException("Cannot find class '" + className + "'.", ex);
            }
        } else {
            klass = arguments[0].getClass();
            object = arguments[0];
        }
        return new KlassAndObject(klass, object);
    }

    record KlassAndObject(Class<?> klass, Object object) {
    }

}
