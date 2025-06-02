package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.commands.BreakCommand;
import ch.turic.Command;

public class BreakAnalyzer extends AbstractAnalyzer {
    public static final BreakAnalyzer INSTANCE = new BreakAnalyzer();

    @Override
    public Command _analyze(LexList lexes) throws BadSyntax {
        return BrReYiAnalyzer.analyze(lexes, BreakCommand::new);
    }
}
