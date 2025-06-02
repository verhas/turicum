package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.Command;
import ch.turic.commands.Operation;

public class UnaryExpressionAnalyzer extends AbstractAnalyzer {
    static final Analyzer INSTANCE = new UnaryExpressionAnalyzer();

    static final String[] unaryOperators = {"+", "-", "~", "!", ".."};

    public Command _analyze(LexList lexes) throws BadSyntax {
        if (lexes.is(unaryOperators)) {
            final var op = lexes.next().text();
            return new Operation(op, null, analyze(lexes));
        }
        return PrimaryExpressionAnalyzer.INSTANCE.analyze(lexes);
    }
}
