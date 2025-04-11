package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import javax0.turicum.ExecutionException;
import javax0.turicum.commands.Command;

import java.util.ArrayList;

public class AssignmentList {
    public static final AssignmentList INSTANCE = new AssignmentList();

    public record Assignment(String identifier, String[] types, Command expression) {
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
        final var pairs = new ArrayList<Assignment>();
        while (lexes.peek().type() == Lex.Type.IDENTIFIER) {
            final var identifier = lexes.next();
            final var type = new ArrayList<String>();
            if (lexes.is(":")) { // process types, optional
                lexes.next();
                ExecutionException.when(!lexes.isIdentifier() && !lexes.isKeyword(), "following the ':' the types identifier has to follow");
                type.add(lexes.next().text());
                while (lexes.is("|")) {
                    lexes.next();
                    ExecutionException.when(!lexes.isIdentifier() && !lexes.isKeyword(), "following the '|' a types identifier has to follow");
                    type.add(lexes.next().text());
                }
            }
            Command expression;
            if (lexes.is("=")) {
                lexes.next();
                expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
            } else {
                expression = null;
            }
            pairs.add(new Assignment(identifier.text(), type.toArray(String[]::new), expression));
            if (lexes.is(",")) {
                lexes.next();
                BadSyntax.when(lexes, !lexes.isIdentifier(), "Identifier missing after , ");
            } else {
                break;
            }
        }
        return pairs.toArray(Assignment[]::new);
    }
}
