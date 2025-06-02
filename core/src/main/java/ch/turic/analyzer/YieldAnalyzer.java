package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.Command;
import ch.turic.commands.YieldCommand;

public class YieldAnalyzer extends AbstractAnalyzer {
    public static final YieldAnalyzer INSTANCE = new YieldAnalyzer();

    @Override
    public Command _analyze(LexList lexes) throws BadSyntax {
        final YieldCommand command =  (YieldCommand) BrReYiAnalyzer.analyze(lexes, YieldCommand::new);
        if( command.expression() == null ){
            throw lexes.syntaxError("Missing expression for yield command");
        }
        return command;
    }
}
