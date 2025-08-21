package ch.turic;

import java.util.List;

/**
 * Represents a function in the Turi language system that can be loaded as a service.
 * This interface extends both LngCallable.LngCallableClosure for function execution
 * capabilities and ServiceLoaded for service loading functionality.
 */
public interface TuriFunction extends LngCallable.LngCallableClosure, ServiceLoaded, SnakeNamed {

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

}
