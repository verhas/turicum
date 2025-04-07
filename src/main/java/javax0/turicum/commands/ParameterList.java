package javax0.turicum.commands;

import javax0.turicum.ExecutionException;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Describes the parameter list of a closure or function or macro or anything that can have parameters.
 * <p>
 * Classes, functions and closures can declare formal parameters, like:
 * <pre>
 *     {@code
 *     fn myFunction(!a,b,c=13,@d,[rest],{meta},|closure|): body
 *     }
 * </pre>
 * <p>
 * In the above example
 * <ul>
 * <li>{@code a} is a positional parameter and cannot be specified as named parameter.
 * <li>{@code b} is a positional or named. It can be specified as the second argument expression or as {@code b=...}
 * <li>{@code c} is also positional or named, and also optional, because it has a default expression.
 * <li>{@code d} is a named expression, does not have a default value, it has to be specified when the function is
 * called as {@code d=...}
 * <li>{@code rest} will become a list holding all the extra positional parameters
 * <li>{@code meta} will become a classless object list holding all the extra named parameters. Note that an extra named
 * parameter may use the identifier as a positional parameter, in the example {@code a}.
 * <li>{@code closure} will hold the value of the last parameter that MUST be a closure or function or something that
 * itself {@link HasParametersWrapped}
 * </ul>
 * <p>
 *     Describing this structure, each parameter has a
 * <ul>
 *     <li>{@code identifier} that identifies it
 *     <li>{@code type} which is {@link ParameterList.Parameter.Type#POSITIONAL_ONLY POSITIONAL},
 *     {@link ParameterList.Parameter.Type#NAMED_ONLY NAMED}, or
 *     {@link ParameterList.Parameter.Type#POSITIONAL_OR_NAMED ANY}
 *     <li>{@code types} that define the types of the parameter or null or empty string if no type is defined
 *     <li>{@code defaultExpression} or null if there is no default expression, which means the parameter is mandatory
 * </ul>
 */
public record ParameterList(Parameter[] parameters, String rest, String meta, String closure) {
    public record Parameter(String identifier,
                            Type type,
                            String[] types,
                            Command defaultExpression) {
        public enum Type {
            POSITIONAL_ONLY, NAMED_ONLY, POSITIONAL_OR_NAMED
        }
    }

    /**
     * Check that the parameter list meets the certain requirements that cannot be enforced by the data structure
     * itself:
     *
     * <ul>
     *     <li>all names are unique
     * </ul>
     * @param parameters the parameter array
     * @param rest the rest parameter name or null
     * @param meta the meta parameter name or null
     * @param closure the closure parameter name or null
     */
    public ParameterList {
        final var others = Arrays.stream(parameters).map(Parameter::identifier).toArray(String[]::new);
        ExecutionException.when(violatesUniqueName(rest, meta, closure, others) ||
                violatesUniqueName(closure, rest, meta, others) ||
                violatesUniqueName(meta, closure, rest, others) ||
                violatesUniqueName(others), "The parameter names have to be unique in a single declaration");
    }


    /**
     * Checks that the identifiers do not violate the rule that each identifier has to be unique.
     */
    private static boolean violatesUniqueName(String[] others) {
        final var set = new HashSet<String>();
        for (final var other : others) {
            if (set.contains(other)) return true;
            set.add(other);
        }
        return false;
    }

    /**
     * Checks if the identifier violates the rule that each identifier has to be unique.
     *
     * @param identifier the identifier to check, 'rest', 'meta' or 'closure'
     * @param other1     one of the other two variables
     * @param other2     the other one of the two other variables
     * @param others     the ordinary parameter variables
     * @return {@code false} if the identifier does not equal any of the other variables given in the array or
     * individually
     */
    private static boolean violatesUniqueName(String identifier, String other1, String other2, String[] others) {
        if (identifier == null) return false;
        if (identifier.equals(other1) || identifier.equals(other2)) return true;
        return Arrays.stream(others).filter(identifier::equals).findFirst().isPresent();
    }

}
