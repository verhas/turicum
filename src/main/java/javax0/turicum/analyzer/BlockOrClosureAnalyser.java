package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import javax0.turicum.commands.Command;

public class BlockOrClosureAnalyser implements Analyzer {

    public static final BlockOrClosureAnalyser INSTANCE = new BlockOrClosureAnalyser();

    @Override
    public Command analyze(Lex.List lexes) throws BadSyntax {
        if (ClosureAnalyzer.blockStartsClosure(lexes)) {
            lexes.next();
            return ClosureAnalyzer.INSTANCE.analyze(lexes);
        }
        return BlockAnalyzer.INSTANCE.analyze(lexes);
    }

}
