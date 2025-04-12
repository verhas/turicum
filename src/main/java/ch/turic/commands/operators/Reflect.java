package ch.turic.commands.operators;

import ch.turic.ExecutionException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;

/**
 * Utility class to perform reflective method invocation on objects for unary and binary operations.
 * <p>
 * This class provides static factory methods to look up instance methods using reflection,
 * ensuring that only non-static, non-synthetic methods are considered.
 * It supports both unary (no argument) and binary (single argument) methods and wraps them
 * into {@link Op} implementations for invocation.
 */
class Reflect {

    private final Method method;
    private final Object op1, op2;
    private final String name; // only to report errors

    /**
     * Private constructor to create a {@code Reflect} object wrapping the method and operands.
     *
     * @param method the method to be invoked
     * @param op1    the object on which the method is invoked
     * @param op2    the argument to be passed to the method, or {@code null} for unary operations
     * @param name   the name of the method, used for error reporting
     */
    private Reflect(Method method, Object op1, Object op2, String name) {
        this.method = method;
        this.op1 = op1;
        this.op2 = op2;
        this.name = name;
    }

    /**
     * Attempts to retrieve a binary instance method from {@code op1}'s class with the given name,
     * accepting a single argument of {@code op2}'s class types.
     * <p>
     * The method must not be static or synthetic.
     *
     * @param name the method name
     * @param op1  the target object
     * @param op2  the parameter to be passed
     * @return an {@link Optional} containing a callable {@link Op} if a suitable method is found, otherwise empty
     */
    static Optional<Reflect.Op> getBinaryMethod(String name, Object op1, Object op2) {
        try {
            final var method = op1.getClass().getMethod(name, op2.getClass());
            if (method.isSynthetic() || Modifier.isStatic(method.getModifiers())) {
                return Optional.empty();
            }
            return Optional.of(new Reflect(method, op1, op2, name).new Binary());
        } catch (NoSuchMethodException e) {
            return Optional.empty();
        }
    }

    /**
     * Attempts to retrieve a unary instance method from {@code op}'s class with the given name.
     * <p>
     * The method must not accept any parameters and must not be static or synthetic.
     *
     * @param name the method name
     * @param op   the target object
     * @return an {@link Optional} containing a callable {@link Op} if a suitable method is found, otherwise empty
     */
    static Optional<Reflect.Op> getUnaryMethod(String name, Object op) {
        try {
            final var method = op.getClass().getMethod(name);
            if (method.isSynthetic() || Modifier.isStatic(method.getModifiers())) {
                return Optional.empty();
            }
            return Optional.of(new Reflect(method, op, null, name).new Unary());
        } catch (NoSuchMethodException e) {
            return Optional.empty();
        }
    }

    /**
     * Interface representing a method invocation operation.
     */
    interface Op {
        Object callMethod() throws ExecutionException;
    }


    /**
     * Represents a binary operation (a method with one parameter).
     */
    class Binary implements Op{
        public Object callMethod() throws ExecutionException {
            try {
                return method.invoke(op1, op2);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new ExecutionException("Error calling the method " + name + " on " + op1.getClass().getName(), e);
            }
        }
    }
    /**
     * Represents a unary operation (a method with no parameters).
     */
    class Unary implements Op{
        public Object callMethod() throws ExecutionException {
            try {
                return method.invoke(op1);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new ExecutionException("Error calling the method " + name + " on " + op1.getClass().getName(), e);
            }
        }
    }

}
