package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.commands.Command;

public class BlockOrClosureAnalyzer extends AbstractAnalyzer {
    public static final BlockOrClosureAnalyzer INSTANCE = new BlockOrClosureAnalyzer();

    @Override
    public Command _analyze(LexList lexes) throws BadSyntax {
        if (ClosureAnalyzer.blockStartsClosure(lexes)) {
            lexes.next();
            return ClosureAnalyzer.INSTANCE.analyze(lexes);
        }
        return BlockAnalyzer.INSTANCE.analyze(lexes);
    }

}
