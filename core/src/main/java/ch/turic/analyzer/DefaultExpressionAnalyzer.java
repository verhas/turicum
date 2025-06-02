package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.Command;

/**
 * Same as {@link ExpressionAnalyzer} but does not allow {@code |} binary or.
 *
 */

public class DefaultExpressionAnalyzer extends AbstractAnalyzer {
    public final static Analyzer INSTANCE = new DefaultExpressionAnalyzer();

    public Command _analyze(LexList lexes) throws BadSyntax {
        return BinaryExpressionAnalyzer.BINARY_DEFAULT_EXPRESSION_ANALYZER.analyze(lexes);
    }
}
