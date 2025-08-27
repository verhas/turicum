package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;

import java.lang.reflect.InvocationTargetException;

/**
 * This class represents an implementation of the TuriFunction interface, designed to
 * dynamically load and create instances of a class by its name. The class leverages
 * the Java reflection API to load classes at runtime.
 *
 * The object returned can be called to call static methods of the class as well as access static fields.
 *
 * <pre>{@code
 * let m = java_class("java.lang.Math");
 * println m.abs(-5)
 * println m.absExact(int(-5))
 * }</pre>
 */
public class JavaClass implements TuriFunction {

    /**
     * Dynamically loads a class with the given name and creates a wrapper instance.
     *
     * @param ctx       The execution context for the call.
     * @param arguments An array of arguments where the first argument is expected
     *                  to be the fully qualified name of the class to load.
     * @return An instance of {@code ch.turic.memory.JavaClass} wrapping the loaded class.
     * @throws ExecutionException If the class cannot be found or any error occurs during class loading.
     */
    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        final var args = FunUtils.args(name(), arguments, String.class, Object[].class);
        final var className = args.at(0).as(String.class);
        try {
            return new ch.turic.memory.JavaClass(Class.forName(className));
        } catch (ClassNotFoundException e) {
            throw new ExecutionException("Could not load class " + className, e);
        }
    }
}
