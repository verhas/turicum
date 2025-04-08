package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import javax0.turicum.commands.Command;
import javax0.turicum.commands.YieldCommand;

public class YieldAnalyzer implements Analyzer {
    public static final YieldAnalyzer INSTANCE = new YieldAnalyzer();

    @Override
    public Command analyze(LexList lexes) throws BadSyntax {
        return BrReYiAnalyzer.analyze(lexes, YieldCommand::new);
    }
}
