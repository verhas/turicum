package ch.turic;

import java.util.List;

/**
 * Represents a macro functionality within the Turi language, defining reusable commands
 * that can be executed with a given context and arguments.
 * <p>
 * This interface extends multiple functionalities:
 * - LngCallable.LngCallableMacro: Indicates that TuriMacro is a callable macro within
 * the context of the Turi runtime.
 * - ServiceLoaded: Enables dynamic discovery and loading of macro implementations using
 * the service provider mechanism.
 * - SnakeNamed: Provides a mechanism to convert macro names into snake_case format.
 * <p>
 * Implementations of this interface are expected to provide:
 * - A `name` method that defines the identifier for the macro.
 * - A `call` method to execute the macro's functionality with the provided context and parameters.
 * <p>
 * Static methods:
 * - getInstances(): Retrieves a list of loaded instances of TuriMacro implementations
 * using the service provider mechanism.
 */
public interface TuriMacro extends LngCallable.LngCallableMacro, ServiceLoaded, SnakeNamed {

    static List<TuriMacro> getInstances() {
        return ServiceLoaded.getInstances(TuriMacro.class);
    }

    /**
     * Similar to {@link #getInstances()}, but uses the specified class loader.
     *
     * @param cl the class loader to use for loading the service implementations
     * @return a list of instances of classes implementing the {@code TuriClass} interface
     */
    static List<TuriMacro> getInstances(ClassLoader cl) {
        return ServiceLoaded.getInstances(TuriMacro.class, cl);
    }
}
