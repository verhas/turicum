package ch.turic;

import ch.turic.utils.StringUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public interface SnakeNamed {
    @Target(java.lang.annotation.ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Name {
        String value();
    }

    /**
     * Returns the identifier name of this function used for registration in the global heap space.
     * This name is used to reference and call the function within the Turi language environment.
     * The name must be unique within the scope where the function is registered.
     * <p>
     * The default implementation uses annotation and reflection, which may seem a performance issue, but
     * this method is called when the methods are registered and not when used.
     *
     * @return the unique name of the function used for registration and reference in the global space
     */
    default String name() {
        final var ann = this.getClass().getAnnotation(Name.class);
        if (ann != null) {
            return ann.value();
        }
        return StringUtils.toSnakeCase(this.getClass().getSimpleName());
    }

}
