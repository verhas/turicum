package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.commands.Assignment;
import ch.turic.commands.Command;

public class AssignmentAnalyzer extends AbstractAnalyzer {
    public static final AssignmentAnalyzer INSTANCE = new AssignmentAnalyzer();

    @Override
    public Command _analyze(LexList lexes) throws BadSyntax {
        final var leftValue = LeftValueAnalyzer.INSTANCE.analyze(lexes);
        if (leftValue == null) {
            return null;
        }
        if (!lexes.hasNext() || !(lexes.peek().type() == Lex.Type.RESERVED) || !lexes.peek().text().equals("=")) {
            return null;
        }
        lexes.next();
        final var expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
        return new Assignment(leftValue, expression);
    }
}
