package ch.turic.commands;

import ch.turic.BadSyntax;
import ch.turic.Command;
import ch.turic.analyzer.Pos;
import ch.turic.memory.Context;
import ch.turic.utils.Unmarshaller;

import java.lang.reflect.Parameter;
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

public record ParameterList(Parameter[] parameters, Identifier rest, Identifier meta, Identifier closure,
                            Pos position) {
    public static final ParameterList EMPTY = new ParameterList(new ParameterList.Parameter[0], null, null, null, new Pos("", null));

    public static ParameterList factory(final Unmarshaller.Args args) {
        return new ParameterList(
                args.get("parameters", Parameter[].class),
                args.get("rest", Identifier.class),
                args.get("meta", Identifier.class),
                args.get("closure", Identifier.class),
                args.get("position", Pos.class)
        );
    }

    /**
     * Creates a new ParameterList with all identifiers bound to their resolved names in the given context.
     * This method is used during function definition execution to resolve any interpolated parameter names to their
     * actual values in the current execution context.
     *
     * <p>The method creates:
     * <ul>
     *     <li>New Identifiers for rest, meta, and closure parameters (if they exist)</li>
     *     <li>New Parameter instances for each regular parameter with resolved identifier names</li>
     * </ul>
     *
     * <p>The resulting ParameterList maintains all the original parameter types, type declarations,
     * and default expressions, only updating the identifier names based on the context.
     *
     * @param context the execution context in which to resolve identifier names
     * @return a new ParameterList with all identifiers bound to their resolved names
     */
    public ParameterList bind(Context context) {
        final var rest = this.rest == null ? null : new Identifier(this.rest.name(context));
        final var meta = this.meta == null ? null : new Identifier(this.meta.name(context));
        final var closure = this.closure == null ? null : new Identifier(this.closure.name(context));
        final var parameters = new Parameter[this.parameters.length];
        for (int i = 0; i < this.parameters.length; i++) {
            parameters[i] = new Parameter(new Identifier(this.parameters[i].identifier().name(context)),
                    this.parameters[i].type(), this.parameters[i].types(),this.parameters[i].defaultExpression);
        }
        return new ParameterList(parameters,rest,meta,closure,this.position);
    }

    public record Parameter(Identifier identifier,
                            Type type,
                            TypeDeclaration[] types,
                            Command defaultExpression) {

        public static Parameter factory(final Unmarshaller.Args args) {
            return new Parameter(
                    args.get("identifier", Identifier.class),
                    args.get("type", Type.class),
                    args.get("types", TypeDeclaration[].class),
                    args.command("defaultExpression")
            );
        }

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
        final var others = Arrays.stream(parameters).map(Parameter::identifier).toArray(Identifier[]::new);
        BadSyntax.when(position, violatesUniqueName(rest, meta, closure, others) ||
                violatesUniqueName(closure, rest, meta, others) ||
                violatesUniqueName(meta, closure, rest, others) ||
                violatesUniqueName(others), "The parameter names have to be unique in a single declaration");
    }


    /**
     * Checks that the identifiers do not violate the rule that each identifier has to be unique.
     */
    private static boolean violatesUniqueName(Identifier[] others) {
        final var set = new HashSet<String>();
        for (final var other : others) {
            if (!other.isInterpolated()) {
                if (set.contains(other.pureName())) return true;
                set.add(other.pureName());
            }
        }
        return false;
    }

    /**
     * Checks if the identifier violates the rule that each identifier has to be unique.
     * <p>
     * When an identifier is interpolated, this check cannot be done against that identifier because the
     * actual name is only known during the execution and it depends on the actual context.
     *
     * @param identifier the identifier to check, 'rest', 'meta' or 'closure'
     * @param other1     one of the other two variables
     * @param other2     the other one of the two other variables
     * @param others     the ordinary parameter variables
     * @return {@code false} if the identifier does not equal any of the other variables given in the array or
     * individually
     */
    private static boolean violatesUniqueName(Identifier identifier, Identifier other1, Identifier other2, Identifier[] others) {
        if (identifier == null || identifier.isInterpolated()) return false; // can't decide if interpolated
        if (other1 != null && !other1.isInterpolated() && identifier.pureName().equals(other1.pureName())) return true;
        if (other2 != null && !other2.isInterpolated() && identifier.pureName().equals(other2.pureName())) return true;
        return Arrays.stream(others).filter(other -> !other.isInterpolated())
                .anyMatch(other -> identifier.pureName().equals(other.pureName()));
    }


    public boolean doesNotFitOperator() {
        return parameters.length != 1 || parameters[0].type == Parameter.Type.NAMED_ONLY ||
                rest != null || meta != null || closure != null;
    }

    public boolean doesNotFitModifier() {
        if (parameters.length == 0) {
            return false;
        }
        return parameters.length != 1 || parameters[0].type == Parameter.Type.NAMED_ONLY ||
                rest != null || meta != null || closure != null;
    }


}
