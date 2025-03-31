package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import javax0.turicum.commands.Command;
import javax0.turicum.commands.Operation;

public class BinaryExpressionAnalyzer implements Analyzer {

    static final Analyzer INSTANCE = new BinaryExpressionAnalyzer();

    static final String[][] binaryOperators = {
        { Keywords.OR },
        { "||" },
        { "&&" },
        { ".." },
        { "|" },
        { "^" },
        { "&" },
        { "==", "!=" },
        { "<", "<=", ">", ">=" },
        { "<<", ">>" },
        { "+", "-" },
        { "*", "/", "%" }
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
            final var op = lexes.next().text();
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
