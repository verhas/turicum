package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import javax0.turicum.commands.Assignment;
import javax0.turicum.commands.Command;

public class AssignmentAnalyzer implements Analyzer {
    public static final AssignmentAnalyzer INSTANCE = new AssignmentAnalyzer();

    @Override
    public Command analyze(final LexList lexes) throws BadSyntax {
        final var leftValue = LeftValueAnalyser.INSTANCE.analyze(lexes);
        final var opSymbol = lexes.next();
        BadSyntax.when(!(opSymbol.type() == Lex.Type.RESERVED) || !opSymbol.text().equals("="),
                "Expected '=' or ':=' after the left value but got '%s'", opSymbol.text());
        final var expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
        return new Assignment(leftValue, expression);
    }
}
