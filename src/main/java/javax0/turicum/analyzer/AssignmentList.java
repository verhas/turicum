package javax0.turicum.analyzer;

import javax0.turicum.commands.Command;

import java.util.ArrayList;

public class AssignmentList {
    public static final AssignmentList INSTANCE = new AssignmentList();

    public record Pair(String identifier, Command expression) {
    }


    public Pair[] analyze(final Lex.List lexes) throws BadSyntax {
        final var pairs = new ArrayList<AssignmentList.Pair>();
        while (lexes.peek().type == Lex.Type.IDENTIFIER) {
            final var identifier = lexes.next();
            Command expression;
            if (lexes.is("=")) {
                lexes.next();
                expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
            } else {
                expression = null;
            }
            pairs.add(new Pair(identifier.text, expression));
            if (lexes.is(",")) {
                lexes.next();
                BadSyntax.when(!lexes.isIdentifier(), "Identifier missing after , ");
            } else {
                break;
            }
        }
        return pairs.toArray(AssignmentList.Pair[]::new);
    }
}
