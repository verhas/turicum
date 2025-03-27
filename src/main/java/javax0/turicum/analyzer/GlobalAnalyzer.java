package javax0.turicum.analyzer;

import javax0.turicum.commands.Command;
import javax0.turicum.commands.GlobalAssignment;

public class GlobalAnalyzer implements Analyzer {
    public static final GlobalAnalyzer INSTANCE = new GlobalAnalyzer();

    @Override
    public Command analyze(Lex.List lexes) throws BadSyntax {
        AssignmentList.Pair[] pairs = AssignmentList.INSTANCE.analyze(lexes);
        return new GlobalAssignment(pairs);
    }
}

