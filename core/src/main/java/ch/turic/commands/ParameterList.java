package ch.turic.commands;

import ch.turic.BadSyntax;
import ch.turic.analyzer.Pos;
import ch.turic.utils.Unmarshaller;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Represents the parameter list of a closure, function, lazy, or any callable entity that can declare parameters.
 *
 * <p>This class is a semantic data holder only. It assumes that syntax analysis has already validated the structure,
 * order, and legality of parameters. It does not perform syntactic validation, such as enforcing ordering rules
 * or ensuring correct default usage.</p>
 *
 * <p>For example, a parameter list like the following:
 * <pre>{@code
 * fn myFunction(!a, b, c = 13, @d, [rest], {meta}, |closure|): body
 * }</pre>
 * will be represented as:
 * <ul>
 *     <li>{@code a} is positional-only (must be passed positionally)</li>
 *     <li>{@code b} is positional or named, and required</li>
 *     <li>{@code c} is positional or named, and optional (has a default expression)</li>
 *     <li>{@code d} is keyword-only, and required (has no default)</li>
 *     <li>{@code rest} will collect any extra positional arguments into a list</li>
 *     <li>{@code meta} will collect any unmatched named arguments into an object (classless)</li>
 *     <li>{@code closure} will hold the last argument, which must be a callable
 *         (a closure, function, lazy, or anything implementing {@link ClosureOrMacro})</li>
 * </ul>
 *
 * <p>Each regular parameter is represented as a {@link Parameter} object, which includes:
 * <ul>
 *     <li>{@code identifier}: the parameter's name</li>
 *     <li>{@code type}: whether the parameter is {@code POSITIONAL}, {@code NAMED}, or {@code ANY}</li>
 *     <li>{@code types}: a type declaration, if any (maybe null or empty)</li>
 *     <li>{@code defaultExpression}: a default value, or null if the parameter is required</li>
 * </ul>
 *
 * <p>The parameters {@code rest}, {@code meta}, and {@code closure} are stored separately and are optional.
 * If present, their names must be unique and not conflict with any regular parameter identifiers.</p>
 */

public record ParameterList(Parameter[] parameters, String rest, String meta, String closure, Pos position) {
    public static final ParameterList EMPTY = new ParameterList(new ParameterList.Parameter[0], null, null, null, new Pos("", null));

    public static ParameterList factory(final Unmarshaller.Args args) {
        return new ParameterList(
                args.get("parameters", Parameter[].class),
                args.str("rest"),
                args.str("meta"),
                args.str("closure"),
                args.get("position", Pos.class)
        );
    }


    public record Parameter(String identifier,
                            Type type,
                            TypeDeclaration[] types,
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
     *
     * @param parameters the parameter array
     * @param rest       the 'rest' parameter name or null
     * @param meta       the 'meta' parameter name or null
     * @param closure    the 'closure' parameter name or null
     */
    public ParameterList {
        final var others = Arrays.stream(parameters).map(Parameter::identifier).toArray(String[]::new);
        BadSyntax.when(position, violatesUniqueName(rest, meta, closure, others) ||
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
        return Arrays.asList(others).contains(identifier);
    }


    public boolean fitOperator() {
        return parameters.length == 1 && parameters[0].type != Parameter.Type.NAMED_ONLY &&
                rest == null && meta == null && closure == null;
    }

    public boolean fitModifier() {
        if (parameters.length == 0) {
            return true;
        }
        return parameters.length == 1 && parameters[0].type != Parameter.Type.NAMED_ONLY &&
                rest == null && meta == null && closure == null;
    }


}
