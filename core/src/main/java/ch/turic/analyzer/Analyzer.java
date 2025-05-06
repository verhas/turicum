package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.commands.Command;

/**
 * An analyzer reads the lexical elements, advances the {@link LexList} removing from the start the consumed elements
 * and returns a {@link Command} that can later be used to execute.
 * <p>
 * Analyzers are usually singletons containing an {@code INSTANCE} field that can be used to invoke the method
 * {@link #analyze(LexList)}.
 * <p>
 * Analyzers can eventually invoke each others following the syntax descriptions.
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
        throw lexes.syntaxError( "Command must be terminated by a semicolon or new line");
    }

}
