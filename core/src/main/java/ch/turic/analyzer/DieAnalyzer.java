package ch.turic.analyzer;

import ch.turic.exceptions.BadSyntax;
import ch.turic.Command;
import ch.turic.commands.DieCommand;

public class DieAnalyzer extends AbstractAnalyzer {
    public static final DieAnalyzer INSTANCE = new DieAnalyzer();

    @Override
    public Command _analyze(LexList lexes) throws BadSyntax {
        return BrReYiAnalyzer.analyze(lexes, DieCommand::new);
    }
}
