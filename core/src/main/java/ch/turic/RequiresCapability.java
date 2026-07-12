package ch.turic;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the {@link Capability capabilities} a built-in function, macro, or class needs in
 * order to be registered for a sandboxed session. A built-in without this annotation requires
 * no capability and is always available.
 * <p>
 * The annotation is read reflectively by {@link ServiceLoaded#capabilities()} at registration
 * time, the same pattern the {@link SnakeNamed.Name} annotation uses for names, so the check
 * costs nothing at call time.
 */
@Target(java.lang.annotation.ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresCapability {
    Capability[] value();
}
