package ch.turic.analyzer;

import ch.turic.exceptions.BadSyntax;

import java.util.ArrayList;

/**
 * Read the input to get a list of identifiers.
 * It is used to get all the parent classes of a class.
 * It can be an empty list if the starting lex is not an identifier.
 * Otherwise, it has to be an identifier optionally followed by other identifiers separated by {@code ,} comma.
 */
public class IdentifierList {
    public static final IdentifierList INSTANCE = new IdentifierList();


    public String[] analyze(final LexList lexes) throws BadSyntax {
        final var identifiers = new ArrayList<String>();

        while (lexes.peek().type() == Lex.Type.IDENTIFIER) {
            identifiers.add(lexes.next().text());
            if (lexes.is(",")) {
                lexes.next();
                BadSyntax.when(lexes, !lexes.isIdentifier(), "Identifier missing after , in identifier list");
            } else {
                break;
            }

        }
        return identifiers.toArray(String[]::new);
    }
}
