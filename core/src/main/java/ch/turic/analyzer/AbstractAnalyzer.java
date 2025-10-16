package ch.turic.analyzer;

import ch.turic.exceptions.BadSyntax;
import ch.turic.commands.AbstractCommand;
import ch.turic.Command;

public abstract class AbstractAnalyzer implements Analyzer {

    @Override
    public Command analyze(LexList lexes) throws BadSyntax {
        final var startPosition = lexes.startPosition().clone();
        final var result = (AbstractCommand) _analyze(lexes);
        if (result != null) {
            result.setStartPosition(startPosition);
            final var index = lexes.getIndex();
            final var lastLexIndex =  index > 0 ? index - 1 : index;
            lexes.setIndex(lastLexIndex);
            result.setEndPosition(lexes.endPosition().clone());
            lexes.setIndex(index);
        }
        return result;
    }

    public abstract Command _analyze(final LexList lexes) throws BadSyntax;
}
