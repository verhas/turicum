package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import javax0.turicum.commands.Command;

/**
 * Same as {@link ExpressionAnalyzer} but does not allow {@code |} binary or.
 *
 */

public class DefaultExpressionAnalyzer implements Analyzer {
    public final static Analyzer INSTANCE = new DefaultExpressionAnalyzer();

    public Command analyze(final Lex.List lexes) throws BadSyntax {
        return BinaryExpressionAnalyzer.BINARY_DEFAULT_EXPRESSION_ANALYZER.analyze(lexes);
    }
}
