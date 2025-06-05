package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;

import java.lang.reflect.InvocationTargetException;

/**
 * A function that creates a new Java object using reflection.
 */
public class JavaNewObject implements TuriFunction {
    @Override
    public String name() {
        return "java_object";
    }

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        final var args = FunUtils.args(name(), arguments, String.class, Object[].class);
        final var className = args.at(0).as(String.class);
        try {
            final var klass = Class.forName(className);
            for (final var constructor : klass.getConstructors()) {
                if (constructor.getParameterCount() != arguments.length - 1 || constructor.isSynthetic()) {
                    continue;
                }
                int i = 1;
                for (final var pType : constructor.getParameterTypes()) {
                    if (!pType.isAssignableFrom(arguments[i].getClass())) {
                        break;
                    }
                    i++;
                }
                if (i == arguments.length) {
                    final Object[] javaArgs = args.tail(1);
                    return constructor.newInstance(javaArgs);
                }
            }
            throw new ExecutionException("No suitable constructor found for class " + className);
        } catch (ClassNotFoundException e) {
            throw new ExecutionException("Could not load class " + className, e);
        } catch (InvocationTargetException e) {
            throw new ExecutionException("Could not invoke constructor " + className, e.getCause());
        } catch (InstantiationException e) {
            throw new ExecutionException("Could not instantiate class " + className, e);
        } catch (IllegalAccessException e) {
            throw new ExecutionException("Could not access constructor " + className, e);
        }
    }
}
