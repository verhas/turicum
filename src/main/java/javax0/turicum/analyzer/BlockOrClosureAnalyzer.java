package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import javax0.turicum.commands.Command;

public class BlockOrClosureAnalyzer implements Analyzer {
    public static final BlockOrClosureAnalyzer INSTANCE = new BlockOrClosureAnalyzer();

    @Override
    public Command analyze(LexList lexes) throws BadSyntax {
        if (ClosureAnalyzer.blockStartsClosure(lexes)) {
            lexes.next();
            return ClosureAnalyzer.INSTANCE.analyze(lexes);
        }
        return BlockAnalyzer.INSTANCE.analyze(lexes);
    }

}
