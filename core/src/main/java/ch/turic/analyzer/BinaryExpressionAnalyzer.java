package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.Command;
import ch.turic.commands.If;
import ch.turic.commands.Operation;

import java.util.Arrays;

/**
 * The BinaryExpressionAnalyzer class is responsible for analyzing and parsing binary expressions
 * based on defined precedence levels and operators. It extends AbstractAnalyzer to leverage
 * a structured approach to expression analysis and supports precedence-based parsing of binary
 * operations.
 * <p>
 * This class uses a two-dimensional array of operators, categorized by precedence levels, for parsing
 * and constructing specific operations. It processes expressions recursively based on these precedence
 * levels and creates an operation tree.
 */
public class BinaryExpressionAnalyzer extends AbstractAnalyzer {
    final String[][] binops;

    static final String[][] binaryOperators = {
            // snippet operators
            // this is not included, copied, and checked against modification
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
            {"*", "/", "%"},
            {"**", "##"},
            {"'"}
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
            throw lexes.syntaxError("Expression is empty");
        }

        if (precedenceLevel == N) {
            return UnaryExpressionAnalyzer.INSTANCE.analyze(lexes);
        }

        var left = analyze(precedenceLevel + 1, lexes);
        while (lexes.is(binops[precedenceLevel]) || lexes.is(Keywords.IF)) {
            if (lexes.is(Keywords.IF)) {
                final int startIndex = lexes.getIndex();
                final var kw = lexes.next();
                final var condition = ExpressionAnalyzer.INSTANCE.analyze(lexes);
                final Command elseExpression;
                if (lexes.is(Keywords.ELSE)) {
                    lexes.next();
                    elseExpression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
                } else {
                    if (lexes.is(":", "{") && kw.atLineStart()) {
                        // this was just a sloppy missing ';' at the end of the previous line
                        lexes.setIndex(startIndex);
                        return left;
                    }
                    throw lexes.syntaxError("Expected 'else' after 'if' in expression");
                }
                left = new If(condition, left, elseExpression);
            } else {
                final var op = lexes.next().text();
                final var right = analyze(precedenceLevel + 1, lexes);
                left = new Operation(op, left, right);
            }
        }
        return left;
    }

    @Override
    public Command _analyze(LexList lexes) throws BadSyntax {
        return analyze(0, lexes);
    }
}
