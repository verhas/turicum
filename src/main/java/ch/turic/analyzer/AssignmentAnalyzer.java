package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.commands.Assignment;
import ch.turic.commands.Command;

public class AssignmentAnalyzer extends AbstractAnalyzer {
    public static final AssignmentAnalyzer INSTANCE = new AssignmentAnalyzer();

    @Override
    public Command _analyze(LexList lexes) throws BadSyntax {
        final var leftValue = LeftValueAnalyser.INSTANCE.analyze(lexes);
        final var opSymbol = lexes.next();
        BadSyntax.when(lexes, !(opSymbol.type() == Lex.Type.RESERVED) || !opSymbol.text().equals("="),
                "Expected '=' after the left value but got '%s'", opSymbol.text());
        final var expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
        return new Assignment(leftValue, expression);
    }
}
