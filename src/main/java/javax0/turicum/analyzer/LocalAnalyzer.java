package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import javax0.turicum.commands.Command;
import javax0.turicum.commands.LocalAssignment;

public record LocalAnalyzer(boolean freeze) implements Analyzer {
    public static final LocalAnalyzer INSTANCE = new LocalAnalyzer(false);
    public static final LocalAnalyzer FINAL_INSTANCE = new LocalAnalyzer(true);

    @Override
    public Command analyze(Lex.List lexes) throws BadSyntax {
        AssignmentList.Pair[] pairs = AssignmentList.INSTANCE.analyze(lexes);
        return new LocalAssignment(pairs, freeze);
    }
}
