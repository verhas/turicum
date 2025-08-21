package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.memory.LngList;

import java.lang.reflect.InvocationTargetException;

import static ch.turic.builtins.functions.JavaCall.getKlassAndObject;

/**
 * A function that calls a Java method.
 */
public class JavaCallVararg implements TuriFunction {

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        final var args = FunUtils.args(name(), arguments, Object.class, String.class, Object[].class);
        final var varargList = args.last().as(LngList.class);
        final var methodName = args.at(1).as(String.class);
        final var tuple = getKlassAndObject(args);
        final var klass = tuple.klass();
        final var object = tuple.object();

        for (final var method : klass.getMethods()) {
            if (!method.getName().equals(methodName) || method.isSynthetic() || !method.isVarArgs()) {
                continue;
            }
            if (method.getParameterCount() != args.N - 2) {
                continue;
            }
            int i = 2;
            for (int j = 0; j < method.getParameterTypes().length - 1; j++, i++) {
                final var pType = method.getParameterTypes()[j];
                if (!pType.isAssignableFrom(args.at(i).type)) {
                    break;
                }
                final var lastPType = method.getParameterTypes()[method.getParameterTypes().length - 1].arrayType();
                while (i < args.N - 1) {
                    if (!lastPType.isAssignableFrom(args.at(i).type)) {
                        break;
                    }
                    i++;
                }
            }
            if (i == args.N - 1) {
                try {
                    final Object[] javaArgs = new Object[method.getParameterCount()];
                    int k = 2, h = 0;
                    while (h < method.getParameterCount() - 1) {
                        javaArgs[h++] = args.at(k++).get();
                    }
                    Class<?> varargComponentType = method.getParameterTypes()[method.getParameterCount() - 1].getComponentType();
                    Object varargs = java.lang.reflect.Array.newInstance(varargComponentType, varargList.array.size());
                    for (int v = 0; v < varargList.array.size(); v++) {
                        final Object vararg = varargList.array.get(v);
                        java.lang.reflect.Array.set(varargs, v, vararg);
                    }
                    javaArgs[h] = varargs;
                    return method.invoke(object, javaArgs);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new ExecutionException("Cannot invoke method '" + methodName + "'.", e);
                }
            }
        }
        throw new ExecutionException("Cannot find method '" + methodName + "'.");

    }

}
