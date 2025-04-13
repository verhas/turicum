package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.memory.LngList;

import java.lang.reflect.InvocationTargetException;

import static ch.turic.builtins.functions.JavaMethodCall.getKlassAndObject;

/**
 * A function that calls a Java method.
 */
public class JavaMethodCallVararg implements TuriFunction {
    @Override
    public String name() {
        return "java_call_vararg";
    }

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        ExecutionException.when(arguments.length < 2, "Function %s needs at least two argument.", name());
        if (!(arguments[arguments.length - 1] instanceof LngList varargList)) {
            throw new ExecutionException("The last argument of '%s' must be a list type.", name());
        }
        if (!(arguments[1] instanceof String methodName)) {
            throw new ExecutionException("Function %s needs a method name as a second argument.", name());
        }
        final var klob = getKlassAndObject(arguments);
        final var klass = klob.klass();
        final var object = klob.object();

        for (final var method : klass.getMethods()) {
            if (!method.getName().equals(methodName) || method.isSynthetic() || !method.isVarArgs()) {
                continue;
            }
            if (method.getParameterCount() != arguments.length - 2) {
                continue;
            }
            int i = 2;
            for (int j = 0; j < method.getParameterTypes().length - 1; j++, i++) {
                final var pType = method.getParameterTypes()[j];
                if (!pType.isAssignableFrom(arguments[i].getClass())) {
                    break;
                }
                final var lastPType = method.getParameterTypes()[method.getParameterTypes().length - 1].arrayType();
                while (i < arguments.length-1) {
                    if (!lastPType.isAssignableFrom(arguments[i].getClass())) {
                        break;
                    }
                    i++;
                }
            }
            if (i == arguments.length-1) {
                try {
                    final Object[] args = new Object[method.getParameterCount()];
                    int k = 2, h = 0;
                    while (h < method.getParameterCount() - 1) {
                        args[h++] = arguments[k++];
                    }
                    Class<?> varargComponentType = method.getParameterTypes()[method.getParameterCount() - 1].getComponentType();
                    Object varargs = java.lang.reflect.Array.newInstance(varargComponentType, varargList.array.size());
                    for (int v = 0; v < varargList.array.size(); v++) {
                        final Object vararg = varargList.array.get(v);
                        java.lang.reflect.Array.set(varargs, v, vararg);
                    }
                    args[h] = varargs;
                    return method.invoke(object, args);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new ExecutionException("Cannot invoke method '" + methodName + "'.", e);
                }
            }
        }
        throw new ExecutionException("Cannot find method '" + methodName + "'.");

    }

}
