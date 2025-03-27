package javax0.turicum.analyzer;

import javax0.turicum.commands.Command;

/**
 * An analyser reads the lexical elements, advances the {@link Lex.List} removing from the start the consumed elements
 * and returns a {@link Command} that can later be used to execute.
 * <p>
 * Analysers are usually singletons containing an {@code INSTANCE} field that can be used to invoke the method
 * {@link #analyze(Lex.List)}.
 * <p>
 * Analysers can eventually invoke each others following the syntax descriptions.
 */

public interface Analyzer {
    Command analyze(final Lex.List lexes) throws BadSyntax;

    static void checkCommandTermination(Lex.List lexes) throws BadSyntax {
        if(lexes.isEmpty()) {
            return;
        }
        final var lex = lexes.peek();
        if (lex.type == Lex.Type.RESERVED) {
            if (lex.text.equals(";") && lexes.hasNext()) {
                lexes.next(); // step over the ';'
            }
        }
    }

}
