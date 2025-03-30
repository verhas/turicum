package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import javax0.turicum.commands.Command;
import javax0.turicum.commands.Operation;

public class UnaryExpressionAnalyzer implements Analyzer {
    static final Analyzer INSTANCE = new UnaryExpressionAnalyzer();

    static final String[] unaryOperators = {"+", "-", "!", ".."};

    public Command analyze(final Lex.List lexes) throws BadSyntax {
        if (lexes.is(unaryOperators)) {
            final var op = lexes.next().text();
            return new Operation(op, null, analyze(lexes));
        }
        return PrimaryExpressionAnalyzer.INSTANCE.analyze(lexes);
    }
}
