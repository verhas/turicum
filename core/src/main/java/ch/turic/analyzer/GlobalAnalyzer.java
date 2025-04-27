package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.commands.Command;
import ch.turic.commands.GlobalAssignment;

public class GlobalAnalyzer extends AbstractAnalyzer {
    public static final GlobalAnalyzer INSTANCE = new GlobalAnalyzer();

    @Override
    public Command _analyze(LexList lexes) throws BadSyntax {
        AssignmentList.Assignment[] assignments = AssignmentList.INSTANCE.analyze(lexes);
        return new GlobalAssignment(assignments);
    }
}

