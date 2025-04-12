package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.commands.*;

public class ReturnAnalyzer extends AbstractAnalyzer {
    public static final ReturnAnalyzer INSTANCE = new ReturnAnalyzer();
    @Override
    public Command _analyze(LexList lexes) throws BadSyntax {
        return BrReYiAnalyzer.analyze(lexes, ReturnCommand::new);
    }
}
