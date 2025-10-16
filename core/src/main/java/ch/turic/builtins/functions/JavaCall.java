package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.exceptions.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.utils.Reflection;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;
/*snippet builtin0160

=== `java_call`

This function calls a Java method.

The first argument is either

* the object on which we want to call a method, or

* the name of the class to call a static method on.

The second argument is the name of the method.
The rest of the arguments are passed to the Java method.
The return value is the value returned by the Java method.

This method works with a fixed number of argument functions as well as with vararg methods.
Note that you can call the Java methods on Java objects or on Java class objects directly, like they were Turicum methods.
Use this method only when the name of the method is known only at run-time.

{%S java_call1%}

The example above performs the same call as in the `java_object` example, but using a string to specify the name of the Java method.

end snippet */

/**
 * {@code java_call()} can call a Java method.
 * <p>
 * The first argument to the function is either
 * <ul>
 *     <li>a string representing the name of a class to invoke a static method, or</li>
 *     <li>an object on which we are going to call the method</li>
 * </ul>
 * <p>
 * The second argument is the name of the method we want to call.
 * <p>
 * The rest of the arguments are the values passed to the method to call.
 * <p>
 * Note: you do not call {@code java.lang.String} methods using this function on a string instance.
 *
 * <pre>{@code
 * // you can create Java objects
 * let BigDecimal = java_class("java.math.BigDecimal")
 * let bd1 = BigDecimal("10.50");
 * // BigDecimal extends the Number class
 * die "wrong type" when type(bd1) != "num";
 *
 * let bd2 = BigDecimal("3.25");
 * // that is bd1.add(bd2)
 * let result = java_call(bd1, "add", bd2);
 * die "wrong type" when type(result) != "num";
 *
 * // automatically adds, because the class implements "add"
 * let result2 = bd1 + bd2
 * die "wrong type" when type(result) != "num";
 * die "add differs from +" when result != result2
 *
 * let formatted = result.toString()
 * die "formatted" when formatted != "13.75";
 *
 * // we can call math utilities
 * let square_root = java_call("java.lang.Math", "sqrt", 16.0);
 * // which are also provided in the language anyway
 * die "wrong square root value" when square_root != sqrt(16.0);
 *
 * // Create deterministic UUID for testing
 * let sample_uuid_str = "550e8400-e29b-41d4-a716-446655440000";
 * let uuid = java_call("java.util.UUID", "fromString", sample_uuid_str)
 * // name starts with an extra "java." prefix to separate from turicum types
 * die "" when type(uuid) != "java.java.util.UUID"
 * let string_repr = java_call(uuid, "toString")
 * die "" when string_repr != sample_uuid_str
 * }</pre>
 */
public class JavaCall implements TuriFunction {

    /**
     * Invokes a method dynamically using reflection based on the provided arguments.
     * This method supports both static and instance method calls.
     *
     * @param context   The execution context in which the method call is performed.
     * @param arguments An array of arguments where:
     *                  - arguments[0] is either a class name (for static calls) or an instance of the object.
     *                  - arguments[1] is the name of the method to invoke.
     *                  - arguments[2] and beyond are the parameters to pass to the method.
     * @return The result of the invoked method.
     * @throws ExecutionException If:
     *                            - The specified class cannot be found.
     *                            - The method cannot be located with the given parameters.
     *                            - An error occurs during the invocation of the method.
     */
    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var args = FunUtils.args(name(), arguments, Object.class, String.class, Object[].class);
        final var methodName = args.at(1).as(String.class);
        final var ctx = FunUtils.ctx(context);
        final var params = args.tail(2);
        final Method method;
        final Object object;
        final Class<?> klass;
        if (args.at(0).is_a(String.class)) { // static call
            try {
                klass = ctx.globalContext.classLoader.loadClass(args.at(0).as(String.class));
            } catch (ClassNotFoundException e) {
                throw new ExecutionException("Cannot find class '" + args.at(0).as(String.class) + "'.", e);
            }
            method = Reflection.getStaticMethodForArgs(klass, methodName, params);
            object = null;
        } else { // non-static call
            object = args.at(0).get();
            method = Reflection.getMethodForArgs(object, methodName, params);
            klass = object.getClass();
        }
        if (method == null) {
            throw new ExecutionException("Cannot find method '" + methodName + "' for class '" + klass.getName() + "' with arguments (" +
                    Arrays.stream(params).map(obj -> obj.getClass().getName() + ":" + obj).collect(Collectors.joining(",")) + ")");
        }
        try {
            return Reflection.invoke(method, object, params);
        } catch (Exception e) {
            throw new ExecutionException(e);
        }

    }
}
