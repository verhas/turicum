package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.ExecutionException;
import ch.turic.Command;
import ch.turic.commands.TypeDeclaration;
import ch.turic.utils.Unmarshaller;

import java.util.ArrayList;

/**
 * The AssignmentList class provides methods to analyze and parse sequences of tokens related
 * to variable assignments and type declarations in a lexical token list. It can parse
 * identifiers, types, and associated expressions based on a defined syntax.
 */
public class AssignmentList {
    public static final AssignmentList INSTANCE = new AssignmentList();
    public static final TypeDeclaration[] EMPTY_TYPE = new TypeDeclaration[0];

    public record Assignment(String identifier, TypeDeclaration[] types, Command expression) {
        public static Assignment factory(Unmarshaller.Args args) {
            return new Assignment(
                    args.str("identifier"),
                    args.get("types", TypeDeclaration[].class),
                    args.command("expression")
            );
        }
    }

    /**
     * Analyzes a sequence of tokens representing a comma-separated list of variable assignments or identifiers.
     * <p>
     * Each item in the list is expected to be an identifier optionally followed by an '=' and an expression.
     * For example: {@code x = 1, y, z = foo()} would produce pairs:
     * <ul>
     *     <li>{@code ("x", expression for 1)}</li>
     *     <li>{@code ("y", null)}</li>
     *     <li>{@code ("z", expression for foo())}</li>
     * </ul>
     * <p>
     * If an identifier is followed by '=', an expression is parsed using {@link ExpressionAnalyzer}.
     * If not, the expression is {@code null}. Commas separate items. The method stops when
     * the next token is not an identifier or when the input ends.
     *
     * @param lexes the lexical token list to analyze; must be positioned at the start of an identifier
     * @return an array of {@link Assignment} objects representing the identifier-expression pairs
     * @throws BadSyntax if the syntax is incorrect, such as a missing identifier after a comma
     */
    public Assignment[] analyze(final LexList lexes) throws BadSyntax {
        return analyze(lexes, true);
    }

    public Assignment[] analyze(final LexList lexes, boolean addValues) throws BadSyntax {
        final var pairs = new ArrayList<Assignment>();
        while (lexes.peek().type() == Lex.Type.IDENTIFIER) {
            final var identifier = lexes.next();
            final TypeDeclaration[] type;
            if (lexes.is(":")) {
                lexes.next();
                type = getTheTypeDefinitions(lexes);
            } else {
                type = EMPTY_TYPE;
            }
            Command expression;
            if (lexes.is("=") && addValues) {
                lexes.next();
                expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
            } else {
                expression = null;
            }
            pairs.add(new Assignment(identifier.text(), type, expression));
            if (lexes.is(",")) {
                lexes.next();
                BadSyntax.when(lexes, !lexes.isIdentifier(), "Identifier missing after , ");
            } else {
                break;
            }
        }
        return pairs.toArray(Assignment[]::new);
    }

    /**
     * Parses type definitions from the given lexical token list. The type definitions can either
     * be simple identifiers or complex expressions encapsulated within parentheses. Multiple type
     * definitions can be separated by the pipe ('|') character.
     *
     * @param lexes the lexical token list to analyze, starting at the beginning of the type definition
     * @return an array of {@link TypeDeclaration} objects representing the parsed type definitions
     */
    public static TypeDeclaration[] getTheTypeDefinitions(LexList lexes) {
        final var types = new ArrayList<TypeDeclaration>();
        fetchNextType(lexes, types);
        while (lexes.is("|")) {
            lexes.next();// step over the |
            fetchNextType(lexes, types);
        }
        return types.toArray(TypeDeclaration[]::new);
    }

    /**
     * Parses closure argument type definitions from the given lexical token list. The method starts by
     * fetching the first type definition and then iterates through the list to analyze multiple types,
     * which may be separated by a pipe ('|'). Type definitions can be simple identifiers or other
     * supported complex type expressions.
     * <p>
     * To distinguish between a type alternative separator ('|') and the closing delimiter of a closure
     * argument list ('|'), the method performs a look-ahead check. It considers a '|' as a type
     * separator only if it's followed by either:
     * <ul>
     *     <li>An identifier followed by '|', ',' or '=' (indicating another type definition)</li>
     *     <li>An opening parenthesis '(' (indicating a complex type expression)</li>
     * </ul>
     * Otherwise, the '|' is treated as the closure argument list closing delimiter.
     *
     * @param lexes the lexical token list containing type definitions, starting at the beginning of
     *              the first type or closure argument
     * @return an array of {@link TypeDeclaration} objects representing the closure argument type
     * definitions parsed from the lexical token list
     */
    public static TypeDeclaration[] getTheTypeDefinitionsClosureArgs(LexList lexes) {
        final var types = new ArrayList<TypeDeclaration>();
        fetchNextType(lexes, types);
        // look ahead to be sure it is not the closure parameter closing '|'
        while (lexes.is("|") && ((lexes.isAt(1, Lex.Type.IDENTIFIER) && lexes.isAt(2, "|", ",", "=")) || lexes.isAt(1, "("))) {
            lexes.next();// step over the |
            fetchNextType(lexes, types);
        }
        return types.toArray(TypeDeclaration[]::new);
    }

    public static void fetchNextType(LexList lexes, ArrayList<TypeDeclaration> type) {
        final boolean referenced = lexes.is("(");
        if (referenced) {
            lexes.next();
            final var expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
            ExecutionException.when(lexes.isNot(")"), "Type expression starting with '(' must finish with ')'");
            lexes.next();
            type.add(new TypeDeclaration(null, expression));
        } else {
            ExecutionException.when(!lexes.isIdentifier() && !lexes.isKeyword(), "following the ':' and '|' a type identifier has to follow");
            type.add(new TypeDeclaration(lexes.next().text(), null));
        }
    }

}
