package javax0.genai.pl.analyzer;

import javax0.genai.pl.commands.Command;

/**
 * An analyser reads the lexical elements, advances the {@link Lex.List} removing from the start the consumed elements
 * and returns a {@link Command} that can later be used to execute.
 * <p>
 * Analysers are usually singletons containing an {@code INSTANCE} field that can be used to invoke the method
 * {@link #analyze(Lex.List)}.
 *
 * Analysers can eventually invoke each others following the syntax descriptions.
 */

public interface Analyzer {
    Command analyze(final Lex.List lexes) throws BadSyntax;

    static void checkCommandTermination(Lex.List lexes) throws BadSyntax {
        BadSyntax.when(lexes.isEmpty(), "Command has to be terminated with ; before end of file.");
        final var lex = lexes.peek();
        if (lex.type == Lex.Type.RESERVED) {
            if (lex.text.equals(";") && lexes.hasNext()) {
                lexes.next(); // step over the ';'
            } else if (!lex.text.equals("}")) {
                throw new BadSyntax("Command has to be terminated with ; unless before }.");
            }
        }
    }

}
