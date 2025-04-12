package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.commands.Command;
import ch.turic.commands.LetAssignment;

public class LetAnalyzer extends AbstractAnalyzer {
    public static final LetAnalyzer INSTANCE = new LetAnalyzer();

    @Override
    public Command _analyze(LexList lexes) throws BadSyntax {
        AssignmentList.Assignment[] assignments = AssignmentList.INSTANCE.analyze(lexes);
        return new LetAssignment(assignments);
    }
}
