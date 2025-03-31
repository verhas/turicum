package javax0.turicum;

import java.util.List;

/**
 * Classes implementing this interface and "providing" these classes from the module can implement methods for
 * Java classes. When there is a method call on a value, which is not {@link javax0.turicum.memory.LngObject}, the
 * run-time checks if there is any class implementing this interface registered for the given class (as returned by the
 * method {@link #forClass()}.
 * <p>
 * If there is then {@link #getMethod(Object, String)} will be invoked on that object to get a callable.
 * To create such a callable see the implementation {@link javax0.turicum.builtins.classes.TuriString TuriString}
 * that implements methods callable on normal strings.
 */
public non-sealed interface TuriClass extends ServiceLoaded {

    static List<TuriClass> getInstances() {
        return ServiceLoaded.getInstances(TuriClass.class);
    }

    /**
     * @return the class on which works
     */
    Class<?> forClass();

    LngCallable getMethod(Object target, String identifier) throws ExecutionException;

}
