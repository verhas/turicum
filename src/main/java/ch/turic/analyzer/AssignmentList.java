package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.ExecutionException;
import ch.turic.commands.Command;
import ch.turic.memory.Context;

import java.util.ArrayList;
import java.util.Arrays;

public class AssignmentList {
    public static final AssignmentList INSTANCE = new AssignmentList();

    public record Assignment(String identifier, Type[] types, Command expression) {
        public record Type(String identifier, Command expression) {
            public static String[] calculateTypeNames(final Context ctx, Type[] types) {
                return Arrays.stream(types).map(t -> t.calculateTypeName(ctx)).toArray(String[]::new);
            }
            public String calculateTypeName(Context context) {
                if (expression == null) {
                    return identifier;
                } else {
                    final var tValue = expression.execute(context);
                    return tValue == null ? "none" : tValue.toString();
                }
            }
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
        return analyze(lexes,true);
    }
    public Assignment[] analyze(final LexList lexes, boolean addValues) throws BadSyntax {
        final var pairs = new ArrayList<Assignment>();
        while (lexes.peek().type() == Lex.Type.IDENTIFIER) {
            final var identifier = lexes.next();
            final var type = getTheTypeDefinitions(lexes);
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

    public static Assignment.Type[] getTheTypeDefinitions(LexList lexes) {
        final var types = new ArrayList<Assignment.Type>();
        if (lexes.is(":")) { // process types, optional
            lexes.next();
            fetchNextType(lexes, types);
            while (lexes.is("|")) {
                lexes.next();// step over the |
                fetchNextType(lexes, types);
            }
        }
        return types.toArray(Assignment.Type[]::new);
    }

    public static void fetchNextType(LexList lexes, ArrayList<Assignment.Type> type) {
        final boolean referenced = lexes.is("(");
        if (referenced) {
            lexes.next();
            final var expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
            ExecutionException.when(lexes.isNot(")"), "Type expression starting with '(' must finish with ')'");
            lexes.next();
            type.add(new Assignment.Type(null, expression));
        } else {
            ExecutionException.when(!lexes.isIdentifier() && !lexes.isKeyword(), "following the ':' and '|' a type identifier has to follow");
            type.add(new Assignment.Type(lexes.next().text(), null));
        }
    }
}
