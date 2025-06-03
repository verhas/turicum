package ch.turic;

import java.util.List;

/**
 * Classes implementing this interface and "providing" these classes from the module can implement methods for
 * Java classes. When there is a method call on a value, which is not {@link ch.turic.memory.LngObject}, the
 * run-time checks if there is any class implementing this interface registered for the given class (as returned by the
 * method {@link #forClass()}.
 * <p>
 * If there is then {@link #getMethod(Object, String)} will be invoked on that object to get a callable.
 * To create such a callable see the implementation {@link ch.turic.builtins.classes.TuriString TuriString}
 * that implements methods callable on normal strings.
 */
public interface TuriClass extends ServiceLoaded {

    /**
     * Retrieves a list of instances of classes that implement the {@code TuriClass} interface.
     * These instances are loaded using the service loader mechanism. If no instances are found through
     * the primary approach, a fallback mechanism attempts to load the classes using the {@code META-INF/services} resources.
     *
     * @return a list of instances of classes implementing the {@code TuriClass} interface
     */
    static List<TuriClass> getInstances() {
        return ServiceLoaded.getInstances(TuriClass.class);
    }

    /**
     * @return the class on which works
     */
    Class<?> forClass();

    LngCallable getMethod(Object target, String identifier) throws ExecutionException;

}
