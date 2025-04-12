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
        ExecutionException.when(arguments.length < 2, "Function %s needs at least two argument.", name());
        final Class<?> klass;
        final Object object;
        if (arguments[0] instanceof Class<?>) {// static method, we do not need to call any method on a Java Class object
            klass = (Class<?>) arguments[0];
            object = null;
        } else if (arguments[0] instanceof String className) {// if it is a string then it is a class name, we do not call string methods from Turicum
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
        if (!(arguments[1] instanceof String methodName)) {
            throw new ExecutionException("Function %s needs a method name as a second argument.", name());
        }

        for (final var method : klass.getMethods()) {
            if (method.getParameterCount() != arguments.length - 2 || method.isSynthetic() || !method.getName().equals(methodName)) {
                continue;
            }
            int i = 2;
            for (final var pType : method.getParameterTypes()) {
                if (!pType.isAssignableFrom(arguments[i].getClass())) {
                    break;
                }
                i++;
            }
            if (i == arguments.length) {
                final Object[] args = Arrays.copyOfRange(arguments, 2, arguments.length);
                try {
                    return method.invoke(object, args);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new ExecutionException("Cannot invoke method '" + methodName + "'.", e);
                }
            }
        }
        throw new ExecutionException("Cannot find method '" + methodName + "'.");
    }
}
