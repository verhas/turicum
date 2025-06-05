package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.commands.AbstractCommand;
import ch.turic.Command;

public abstract class AbstractAnalyzer implements Analyzer {

    @Override
    public Command analyze(LexList lexes) throws BadSyntax {
        final var startPosition = lexes.position();
        final var result = (AbstractCommand) _analyze(lexes);
        if (result != null) {
            result.setStartPosition(startPosition);
            result.setEndPosition(lexes.position());
        }
        return result;
    }

    public abstract Command _analyze(final LexList lexes) throws BadSyntax;
}
