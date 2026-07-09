package ch.turic;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the Turicum call signature of a Java-coded builtin function or macro.
 * <p>
 * The value uses the same parameter-list syntax as Turicum functions, without the surrounding
 * parentheses. For example, a builtin can declare a signature such as
 * {@code path: str, @mode = "r", [rest], {meta}, ^body}.
 * Annotated builtins are called through the normal Turicum argument binder, so named
 * arguments, default values, rest/meta parameters, spread arguments, and trailing closures
 * behave like they do for Turicum-coded functions.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TuriParameters {
    String value();
}
