package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;

import java.lang.reflect.InvocationTargetException;

/**
 * The JavaNewObject class implements the TuriFunction interface to provide the ability
 * to dynamically instantiate new Java objects at runtime using reflection. This function
 * is registered within the Turi language system under the name "java_object".
 * <p>
 * The function takes a fully qualified class name as the first argument and an array of
 * constructor arguments as the second argument. It attempts to find a matching constructor
 * in the specified class and create a new instance of the class using the provided arguments.
 * <p>
 * If the class cannot be found, the constructor is inaccessible, or no suitable constructor
 * matches the arguments, an {@link ExecutionException} is thrown.
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
                if (constructor.getParameterCount() != args.N - 1 || constructor.isSynthetic()) {
                    continue;
                }
                int i = 1;
                for (final var pType : constructor.getParameterTypes()) {
                    if (!pType.isAssignableFrom(args.at(i).type)) {
                        break;
                    }
                    i++;
                }
                if (i == args.N) {
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
