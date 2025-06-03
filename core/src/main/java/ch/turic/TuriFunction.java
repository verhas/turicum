package ch.turic;

import java.util.List;

/**
 * Represents a function in the Turi language system that can be loaded as a service.
 * This interface extends both LngCallable.LngCallableClosure for function execution
 * capabilities and ServiceLoaded for service loading functionality.
 */
public interface TuriFunction extends LngCallable.LngCallableClosure, ServiceLoaded {

    /**
     * Returns all instances of TuriFunction that are currently loaded in the system.
     * This method utilizes the ServiceLoader mechanism to discover and load all
     * implementations of TuriFunction available in the classpath.
     *
     * @return a List containing all discovered TuriFunction instances
     */
    static List<TuriFunction> getInstances() {
        return ServiceLoaded.getInstances(TuriFunction.class);
    }

    /**
     * Returns the identifier name of this function used for registration in the global heap space.
     * This name is used to reference and call the function within the Turi language environment.
     * The name must be unique within the scope where the function is registered.
     *
     * @return the unique name of the function used for registration and reference in the global space
     */
    String name();

}
