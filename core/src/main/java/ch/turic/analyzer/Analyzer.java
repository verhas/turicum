package ch.turic.analyzer;

import ch.turic.exceptions.BadSyntax;
import ch.turic.Command;
import ch.turic.Input;

/**
 * An analyzer reads the lexical elements, advances the {@link LexList} removing from the start the consumed elements
 * and returns a {@link Command} that can later be used to execute.
 * <p>
 * Analyzers are usually singletons containing an {@code INSTANCE} field that can be used to invoke the method
 * {@link #analyze(LexList)}.
 * <p>
 * Analyzers can eventually invoke each other following the syntax descriptions.
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
        if (lexes.is(")", "{", "}", ":")) {
            return;
        }
        if (isKeyword(lex)) {
            return;
        }
        if (lex.atLineStart()) {
            return;
        }
        throw lexes.syntaxError("Command must be terminated by a semicolon or new line");
    }

    private static boolean isKeyword(Lex lex) {
        return lex.type() == Lex.Type.RESERVED && !lex.text().isEmpty() && Input.validId1stChar(lex.text().charAt(0));
    }

}
