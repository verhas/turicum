package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.commands.AbstractCommand;
import ch.turic.commands.Command;

public abstract class AbstractAnalyzer implements Analyzer {

    private Pos startPosition;

    public Pos startPosition() {
        return startPosition;
    }

    public Pos endPosition() {
        return endPosition;
    }

    private Pos endPosition;

    @Override
    public Command analyze(LexList lexes) throws BadSyntax {
        startPosition = lexes.position();
        final var result = (AbstractCommand) _analyze(lexes);
        if (result != null) {
            result.setStartPosition(startPosition);
            endPosition = lexes.position();
            result.setEndPosition(endPosition);
        }
        return result;

    }

    public abstract Command _analyze(final LexList lexes) throws BadSyntax;
}
