package javax0.genai.pl.analyzer;

import javax0.genai.pl.commands.Command;
import javax0.genai.pl.commands.Operation;

public class UnaryExpressionAnalyzer implements Analyzer {
    static final Analyzer INSTANCE = new UnaryExpressionAnalyzer();

    static final String[] unaryOperators = {"+", "-", "!"};

    public Command analyze(final Lex.List lexes) throws BadSyntax {
        if (lexes.is(unaryOperators)) {
            final var op = lexes.next().text;
            return new Operation(op, null, analyze(lexes));
        }
        return PrimaryExpressionAnalyzer.INSTANCE.analyze(lexes);
    }
}
