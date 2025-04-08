package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import javax0.turicum.commands.Command;
import javax0.turicum.commands.Operation;

import java.util.Arrays;

public class BinaryExpressionAnalyzer implements Analyzer {
    final String[][] binops;

    static final String[][] binaryOperators = {
            {Keywords.OR}, // 0
            {"||"},        // 1
            {"&&"},        // 2
            {".."},        // 3
            {"|"},         // 4
            {"^"},
            {"&"},
            {"==", "!="},
            {"<", "<=", ">", ">="},
            {"<<", ">>"},
            {"+", "-"},
            {"*", "/", "%"}
    };
    static final BinaryExpressionAnalyzer INSTANCE = new BinaryExpressionAnalyzer(binaryOperators);
    static final BinaryExpressionAnalyzer BINARY_DEFAULT_EXPRESSION_ANALYZER = new BinaryExpressionAnalyzer(Arrays.copyOf(binaryOperators, binaryOperators.length));

    static {
        BINARY_DEFAULT_EXPRESSION_ANALYZER.binops[4] = new String[0];
    }


    private final int N;

    public BinaryExpressionAnalyzer(String[][] binops) {
        this.binops = binops;
        N = binops.length;
    }

    public Command analyze(final int precedenceLevel, final LexList lexes) throws BadSyntax {
        if (lexes.isEmpty()) {
            throw new BadSyntax("Expression is empty");
        }

        if (precedenceLevel == N) {
            return UnaryExpressionAnalyzer.INSTANCE.analyze(lexes);
        }

        var left = analyze(precedenceLevel + 1, lexes);
        while (lexes.is(binops[precedenceLevel])) {
            final var op = lexes.next().text();
            final var right = analyze(precedenceLevel + 1, lexes);
            left = new Operation(op, left, right);
        }
        return left;
    }

    @Override
    public Command analyze(LexList lexes) throws BadSyntax {
        return analyze(0, lexes);
    }
}
