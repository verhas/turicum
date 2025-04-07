package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import javax0.turicum.commands.Command;
import javax0.turicum.commands.LetAssignment;

public record LetAnalyzer() implements Analyzer {
    public static final LetAnalyzer INSTANCE = new LetAnalyzer();

    @Override
    public Command analyze(Lex.List lexes) throws BadSyntax {
        AssignmentList.Assignment[] assignments = AssignmentList.INSTANCE.analyze(lexes);
        return new LetAssignment(assignments, false);
    }
}
