package javax0.turicum.analyzer;

import javax0.turicum.commands.Command;

public class BlockOrClosureAnalyser implements Analyzer {
    public static final BlockOrClosureAnalyser INSTANCE = new BlockOrClosureAnalyser();

    @Override
    public Command analyze(Lex.List lexes) throws BadSyntax {
        if (lexes.isAt(1,"|")) {
            lexes.next();
            lexes.next();
            return ClosureAnalyzer.INSTANCE.analyze(lexes);
        }
        return BlockAnalyzer.INSTANCE.analyze(lexes);
    }
}
