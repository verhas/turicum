package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import javax0.turicum.commands.Assignment;
import javax0.turicum.commands.Command;
import javax0.turicum.commands.ExportAssignment;
import javax0.turicum.memory.VariableLeftValue;

public class AssignmentAnalyzer implements Analyzer {
    public static final AssignmentAnalyzer INSTANCE = new AssignmentAnalyzer();

    @Override
    public Command analyze(final Lex.List lexes) throws BadSyntax {
        final var leftValue = LeftValueAnalyser.INSTANCE.analyze(lexes);
        final var opSymbol = lexes.next();
        BadSyntax.when(!(opSymbol.type()== Lex.Type.RESERVED
                        && (opSymbol.text().equals("=") || opSymbol.text().equals(":="))),
                "Expected '=' or ':=' after the left value but got '%s'", opSymbol.text());
        final var expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
        if (opSymbol.text().equals("=")) {

            return new Assignment(leftValue, expression);
        } else {
            if (leftValue instanceof VariableLeftValue vLeftValue) {
                return new ExportAssignment(vLeftValue, expression);
            } else {
                throw new BadSyntax("Expected variable left value but got '" + leftValue + "'");
            }
        }
    }
}
