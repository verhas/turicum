package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.exceptions.ExecutionException;
import ch.turic.TuriFunction;

/*snippet builtin0170

=== `java_class`

Create a Turicum object that is a Java class itself.
This object can be used as a constructor to create instances of the class and to invoke static methods.

{%S java_class1%}

You can also use these objects to create a new instance of the Java class.
These objects are callable, so you can call them to get a new instance of the underlying Java class:

{%S java_class2%}

The constructor can also be vararg:

{%S java_class3%}

end snippet */

/**
 * This class represents an implementation of the TuriFunction interface, designed to
 * dynamically load and create instances of a class by its name. The class leverages
 * the Java reflection API to load classes at runtime.
 * <p>
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
     * @param context   The execution context for the call.
     * @param arguments An array of arguments where the first argument is expected
     *                  to be the fully qualified name of the class to load.
     * @return An instance of {@code ch.turic.memory.JavaClass} wrapping the loaded class.
     * @throws ExecutionException If the class cannot be found or any error occurs during class loading.
     */
    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var args = FunUtils.args(name(), arguments, String.class, Object[].class);
        final var ctx = FunUtils.ctx(context);
        final var className = args.at(0).as(String.class);
        try {
            final var klass = ctx.globalContext.classLoader.loadClass(className);
            return new ch.turic.memory.JavaClass(klass);
        } catch (ClassNotFoundException e) {
            throw new ExecutionException("Could not load class " + className, e);
        }
    }
}
