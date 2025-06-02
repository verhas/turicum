package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.Command;
import ch.turic.commands.Operation;

import java.util.Arrays;

public class BinaryExpressionAnalyzer extends AbstractAnalyzer {
    final String[][] binops;

    static final String[][] binaryOperators = {
            // snippet operators
            // this is not included, copied and checked against modification
            {Keywords.OR}, // 0
            {"||"},        // 1
            {"&&"},        // 2
            {".."},        // 3
            {"|"},         // 4
            {"^"},
            {"&"},
            {"===", "==", "!=", Keywords.IN},
            {"<", "<=", ">", ">="},
            {"<<", ">>", ">>>"},
            {"+", "-"},
            {"*", "/", "%"}
            // end snippet
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
            throw lexes.syntaxError( "Expression is empty");
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
    public Command _analyze(LexList lexes) throws BadSyntax {
        return analyze(0, lexes);
    }
}
