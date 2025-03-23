package javax0.genai.pl.analyzer;

import javax0.genai.pl.commands.Command;
import javax0.genai.pl.commands.Operation;

public class BinaryExpressionAnalyzer implements Analyzer {
    static final Analyzer INSTANCE = new BinaryExpressionAnalyzer();

    private static final String[][] binaryOperators = {
            {"||"},
            {"&&"},
            {"|"},
            {"^"},
            {"&"},
            {"==", "!="},
            {"<", "<=", ">", ">="},
            {"<<", ">>"},
            {"+", "-"},
            {"*", "/", "%"},
    };
    private static final int N = binaryOperators.length;

    public Command analyze(final int precedenceLevel, final Lex.List lexes) throws BadSyntax {
        if (lexes.isEmpty()) {
            throw new BadSyntax("Expression is empty");
        }

        if (precedenceLevel == N) {
            return UnaryExpressionAnalyzer.INSTANCE.analyze(lexes);
        }

        var left = analyze(precedenceLevel + 1, lexes);
        while (lexes.is(binaryOperators[precedenceLevel])) {
            final var op = lexes.next().text;
            final var right = analyze(precedenceLevel + 1, lexes);
            left = new Operation(op, left, right);
        }
        return left;
    }

    @Override
    public Command analyze(Lex.List lexes) throws BadSyntax {
        return analyze(0, lexes);
    }
}
