package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.Command;
import ch.turic.commands.ParameterList;
import ch.turic.commands.TypeDeclaration;

import java.util.ArrayList;

/**
 * The ParameterDefinition class is used to analyze and process a list of parameters
 * specified in a lexical input. It evaluates parameters based on predefined rules
 * and returns a structured representation of the parameters.
 * <p>
 * The class provides support for various parameter types, including:
 * - Positional-only parameters
 * - Named-only parameters
 * - Positional or named parameters
 * <p>
 * Additionally, the class handles special parameters such as:
 * - Rest parameter (denoted by [rest])
 * - Meta parameter (denoted by {meta})
 * - Closure parameter (denoted by ^closure)
 * <p>
 * The parsing process validates the structure and relationships between these
 * parameters, ensuring consistency and correctness.
 * <p>
 * Users can utilize the provided constants to create instances tailored for
 * different parsing scenarios:
 * - `INSTANCE` for general parameter parsing.
 * - `FOR_CLOSURE` for parsing closure-specific parameters.
 *
 * <pre>
 * snippet EBNF_PARAMETER_LIST
 * PARAMETER_LIST ::= PARAMETER (',' PARAMETER)*
 *
 * PARAMETER ::= NORMAL_PARAMETER | SPECIAL_PARAMETER
 *
 * NORMAL_PARAMETER ::= [PARAMETER_TYPE] IDENTIFIER [TYPE_DECLARATION] [DEFAULT_VALUE]
 *
 * PARAMETER_TYPE ::= '!'  // positional only
 *                 | '@'  // named only
 *
 * SPECIAL_PARAMETER ::= REST_PARAMETER | META_PARAMETER | CLOSURE_PARAMETER
 *
 * REST_PARAMETER ::= '[' IDENTIFIER ']'
 *
 * META_PARAMETER ::= '{' IDENTIFIER '}'
 *
 * CLOSURE_PARAMETER ::= '^' IDENTIFIER
 *
 * TYPE_DECLARATION ::= ':' TYPE_LIST
 *
 * TYPE_LIST ::= IDENTIFIER (',' IDENTIFIER)*
 *
 * DEFAULT_VALUE ::= '=' EXPRESSION
 *
 * // Special parameters must appear in this order, if present
 * PARAMETERS_ORDER ::= NORMAL_PARAMETER* [REST_PARAMETER] [META_PARAMETER] [CLOSURE_PARAMETER]
 * end snippet
 * </pre>
 */
public class ParameterDefinition {
    private final boolean forClosure;
    public static final ParameterDefinition INSTANCE = new ParameterDefinition(false);
    public static final ParameterDefinition FOR_CLOSURE = new ParameterDefinition(true);

    public ParameterDefinition(boolean forClosure) {
        this.forClosure = forClosure;
    }


    public ParameterList analyze(final LexList lexes) throws BadSyntax {
        final var commonParameters = new ArrayList<ParameterList.Parameter>();
        String rest = null;
        String meta = null;
        String closure = null;
        Pos position = lexes.position();

        while (lexes.peek().type() == Lex.Type.IDENTIFIER || lexes.is("@", "[", "!", "{", "^")) {
            final ParameterList.Parameter.Type type;
            if (lexes.is("!")) {
                type = ParameterList.Parameter.Type.POSITIONAL_ONLY;
                lexes.next();
            } else if (lexes.is("@")) {
                lexes.next();
                type = ParameterList.Parameter.Type.NAMED_ONLY;
            } else {
                type = ParameterList.Parameter.Type.POSITIONAL_OR_NAMED;
            }
            boolean extraParam = lexes.is("[", "{", "^");
            BadSyntax.when(lexes, extraParam && type != ParameterList.Parameter.Type.POSITIONAL_OR_NAMED, "The parameter [rest], {meta} or |closure| cannot be named or positional, do not use ! or @ before it.");
            final String id;
            if (extraParam) {
                final var opening = lexes.next().text();
                BadSyntax.when(lexes, !lexes.isIdentifier(), "[rest], {meta} or |closure| opening character needs an identifier, it is missing");
                id = lexes.next().text();
                final var closing = switch (opening) {
                    case "[" -> {
                        BadSyntax.when(lexes, rest != null, "You cannot have more than one [rest] parameter");
                        rest = id;
                        BadSyntax.when(lexes, meta != null || closure != null, "[rest] must not come after {meta} or |closure|.");
                        yield "]";
                    }
                    case "{" -> {
                        BadSyntax.when(lexes, meta != null, "You cannot have more than one {meta} parameter");
                        meta = id;
                        BadSyntax.when(lexes, closure != null, "{meta} must not come after |closure|.");
                        yield "}";
                    }
                    case "^" -> {
                        BadSyntax.when(lexes, closure != null, "You cannot have more than one |closure| parameter");
                        closure = id;
                        yield null;
                    }
                    default -> throw lexes.syntaxError("Something went wrong 7639/a2");
                };
                if (closing != null) {
                    BadSyntax.when(lexes, lexes.isNot(closing), "'%s%s must be followed by %s", opening, id, closing);
                    lexes.next();
                }
                if (lexes.is(",")) {
                    lexes.next();
                    continue;
                } else {
                    break;
                }
            } else {
                BadSyntax.when(lexes, !lexes.isIdentifier(), "Parameter name is expected");
                BadSyntax.when(lexes, rest != null || meta != null || closure != null, "[rest], {meta} , and |clore| can stand only at the end of the parameter list.");
                id = lexes.next().text();
            }
            final TypeDeclaration[] types;
            if (lexes.is(":")) {
                lexes.next();
                types = forClosure ? AssignmentList.getTheTypeDefinitionsClosureArgs(lexes) : AssignmentList.getTheTypeDefinitions(lexes);
            } else {
                types = AssignmentList.EMPTY_TYPE;
            }

            final Command defaultExpression;
            if (lexes.is("=")) {
                lexes.next();
                defaultExpression = DefaultExpressionAnalyzer.INSTANCE.analyze(lexes);
            } else {
                defaultExpression = null;
            }
            commonParameters.add(new ParameterList.Parameter(id, type, types, defaultExpression));
            if (lexes.is(",")) {
                lexes.next();
                BadSyntax.when(lexes, lexes.peek().type() != Lex.Type.IDENTIFIER && lexes.isNot("@", "[", "!", "{", "^"), "Identifier expected after ',' in parameter list");
            } else {
                break;
            }

        }
        return new ParameterList(commonParameters.toArray(ParameterList.Parameter[]::new), rest, meta, closure, position);
    }
}
