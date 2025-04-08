package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import javax0.turicum.commands.Command;
import javax0.turicum.commands.GlobalAssignment;

public class GlobalAnalyzer implements Analyzer {
    public static final GlobalAnalyzer INSTANCE = new GlobalAnalyzer();

    @Override
    public Command analyze(LexList lexes) throws BadSyntax {
        AssignmentList.Assignment[] assignments = AssignmentList.INSTANCE.analyze(lexes);
        return new GlobalAssignment(assignments);
    }
}

