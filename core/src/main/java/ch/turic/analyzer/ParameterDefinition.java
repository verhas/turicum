package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.Command;
import ch.turic.commands.ParameterList;
import ch.turic.commands.TypeDeclaration;

import java.util.ArrayList;

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
                    default -> throw lexes.syntaxError( "Something went wrong 7639/a2");
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
