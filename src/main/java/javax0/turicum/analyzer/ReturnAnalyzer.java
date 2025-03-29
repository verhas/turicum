package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import javax0.turicum.commands.*;

public class ReturnAnalyzer implements Analyzer {
    public static final ReturnAnalyzer INSTANCE = new ReturnAnalyzer();
    @Override
    public Command analyze(Lex.List lexes) throws BadSyntax {
        return BrReYiAnalyzer.analyze(lexes, ReturnCommand::new);
    }
}
