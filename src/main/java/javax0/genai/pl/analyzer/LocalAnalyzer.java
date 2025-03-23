package javax0.genai.pl.analyzer;

import javax0.genai.pl.commands.Command;
import javax0.genai.pl.commands.LocalAssignment;
import javax0.genai.pl.commands.Undefined;
import javax0.genai.pl.memory.VariableLeftValue;

import java.util.Objects;

public record LocalAnalyzer(boolean freeze) implements Analyzer {
    public static final LocalAnalyzer INSTANCE = new LocalAnalyzer(false);
    public static final LocalAnalyzer FINAL_INSTANCE = new LocalAnalyzer(true);

    @Override
    public Command analyze(Lex.List lexes) throws BadSyntax {
        AssignmentList.Pair[] pairs = AssignmentList.INSTANCE.analyze(lexes);
        return new LocalAssignment(pairs, freeze);
    }
}
