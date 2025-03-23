package javax0.genai.pl.analyzer;

import javax0.genai.pl.commands.Assignment;
import javax0.genai.pl.commands.Command;
import javax0.genai.pl.commands.ExportAssignment;
import javax0.genai.pl.memory.VariableLeftValue;

public class AssignmentAnalyzer implements Analyzer {
    public static final AssignmentAnalyzer INSTANCE = new AssignmentAnalyzer();

    @Override
    public Command analyze(final Lex.List lexes) throws BadSyntax {
        final var leftValue = LeftValueAnalyser.INSTANCE.analyze(lexes);
        final var opSymbol = lexes.next();
        BadSyntax.when(!(opSymbol.type == Lex.Type.RESERVED
                        && (opSymbol.text.equals("=") || opSymbol.text.equals(":="))),
                "Expected '=' or ':=' after the left value but got '" + opSymbol.text + "'");
        final var expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
        if (opSymbol.text.equals("=")) {

            return new Assignment(leftValue, expression);
        } else {
            if (leftValue instanceof VariableLeftValue vLeftValue) {
                return new ExportAssignment(vLeftValue, expression);
            }else{
                throw new BadSyntax("Expected variable left value but got '" + leftValue + "'");
            }
        }
    }
}
