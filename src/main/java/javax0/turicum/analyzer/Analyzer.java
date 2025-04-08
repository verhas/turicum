package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import javax0.turicum.commands.Command;

/**
 * An analyser reads the lexical elements, advances the {@link LexList} removing from the start the consumed elements
 * and returns a {@link Command} that can later be used to execute.
 * <p>
 * Analysers are usually singletons containing an {@code INSTANCE} field that can be used to invoke the method
 * {@link #analyze(LexList)}.
 * <p>
 * Analysers can eventually invoke each others following the syntax descriptions.
 */

public interface Analyzer {
    Command analyze(final LexList lexes) throws BadSyntax;

    static void checkCommandTermination(LexList lexes) throws BadSyntax {
        if (lexes.isEmpty()) {
            return;
        }
        final var lex = lexes.peek();
        if (lexes.is(";")) {
            lexes.next(); // step over the ';'
            return;
        }
        if (lex.type() == Lex.Type.RESERVED  || lex.atLineStart()) {
            return;
        }
        throw new BadSyntax("Command must be terminated by a semicolon or new line");
    }

}
