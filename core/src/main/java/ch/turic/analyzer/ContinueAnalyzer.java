package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.Command;
import ch.turic.commands.ContinueCommand;

public class ContinueAnalyzer extends AbstractAnalyzer {
    public static final ContinueAnalyzer INSTANCE = new ContinueAnalyzer();

    @Override
    public Command _analyze(LexList lexes) throws BadSyntax {
        return BrReYiAnalyzer.analyze(lexes, ContinueCommand::new);
    }
}
