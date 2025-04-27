package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.commands.Command;
import ch.turic.commands.YieldCommand;

public class YieldAnalyzer extends AbstractAnalyzer {
    public static final YieldAnalyzer INSTANCE = new YieldAnalyzer();

    @Override
    public Command _analyze(LexList lexes) throws BadSyntax {
        return BrReYiAnalyzer.analyze(lexes, YieldCommand::new);
    }
}
