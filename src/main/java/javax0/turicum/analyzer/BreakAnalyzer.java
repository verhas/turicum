package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import javax0.turicum.commands.BreakCommand;
import javax0.turicum.commands.Command;

public class BreakAnalyzer implements Analyzer {
    public static final BreakAnalyzer INSTANCE = new BreakAnalyzer();

    @Override
    public Command analyze(Lex.List lexes) throws BadSyntax {
        return BrReYiAnalyzer.analyze(lexes, BreakCommand::new);
    }
}
