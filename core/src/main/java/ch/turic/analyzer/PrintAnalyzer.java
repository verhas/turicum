package ch.turic.analyzer;


import ch.turic.BadSyntax;
import ch.turic.commands.Command;
import ch.turic.commands.Print;

import java.util.ArrayList;

public class PrintAnalyzer extends AbstractAnalyzer {
    public static final PrintAnalyzer INSTANCE = new PrintAnalyzer(false);
    public static final PrintAnalyzer INSTANCE_NL = new PrintAnalyzer(true);

    private final boolean nl;

    public PrintAnalyzer(boolean nl) {
        this.nl = nl;
    }

    public Command _analyze(LexList lexes) throws BadSyntax {
        final var expressions = new ArrayList<Command>();
        final boolean enclosed = lexes.is("(");
        if (enclosed) {
            lexes.next();
            if( lexes.is(")")) {
                lexes.next();
                return new Print(new Command[0],nl);
            }
        }

        for (; ; ) {
            final var expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
            if (expression != null) {
                expressions.add(expression);
            }
            if (lexes.isNot(",")) {
                break;
            }
            lexes.next();
        }
        if (enclosed) {
            if (lexes.isNot(")")) {
                throw lexes.syntaxError("parameters following 'print(' should also have a closing ')' or drop the opening '(' also");
            }
            lexes.next();
        }
        return new Print(expressions.toArray(Command[]::new), nl);
    }
}
