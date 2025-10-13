package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.utils.Reflection;

import java.lang.reflect.InvocationTargetException;
/*snippet builtin0180

end snippet */

/**
 * The JavaObject class implements the TuriFunction interface to provide the ability
 * to dynamically instantiate new Java objects at runtime using reflection. This function
 * is registered within the Turi language system under the name "java_object".
 * <p>
 * The function takes a fully qualified class name as the first argument and an array of
 * constructor arguments as the second argument. It attempts to find a matching constructor
 * in the specified class and create a new instance of the class using the provided arguments.
 * <p>
 * If the class cannot be found, the constructor is inaccessible, or no suitable constructor
 * matches the arguments, an {@link ExecutionException} is thrown.
 *
 * This function may be needed in exceptional cases only. The same functionality can be reached
 * calling the {@link JavaClass} function, get the class as an object. When called with arguments
 * it will invoke the appropriate constructor.
 *
 * <pre>{@code
 * let bd1 = java_object("java.math.BigDecimal", "10.50");
 * let BigDecimal = java_class("java.math.BigDecimal");
 * let bd2 = BigDecimal("3.25")
 * let result = bd1.add(bd2)
 * println result // is 13.75 BigDecimal
 * }</pre>
 */
public class JavaObject implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var args = FunUtils.args(name(), arguments, String.class, Object[].class);
        final var ctx = FunUtils.ctx(context);
        final var className = args.at(0).as(String.class);
        final var javaArgs = args.tail(1);
        try {
            final var klass = ctx.globalContext.classLoader.loadClass(className);
            final var constructor = Reflection.getConstructorForArgs(klass, javaArgs);
            return constructor.newInstance(javaArgs);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new ExecutionException(e, "Cannot create an instance of %s with arguments", className);
        }
    }
}
