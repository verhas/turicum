package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import javax0.turicum.commands.Command;
import javax0.turicum.commands.LetAssignment;

public record PinAnalyzer() implements Analyzer {
    public static final PinAnalyzer INSTANCE = new PinAnalyzer();

    @Override
    public Command analyze(Lex.List lexes) throws BadSyntax {
        AssignmentList.Assignment[] assignments = AssignmentList.INSTANCE.analyze(lexes);
        return new LetAssignment(assignments, true);
    }
}
