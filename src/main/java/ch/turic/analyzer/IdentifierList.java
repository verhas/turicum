package ch.turic.analyzer;

import ch.turic.BadSyntax;

import java.util.ArrayList;

public class IdentifierList {
    public static final IdentifierList INSTANCE = new IdentifierList();


    public String[] analyze(final LexList lexes) throws BadSyntax {
        final var identifiers = new ArrayList<String>();

        while (lexes.peek().type()== Lex.Type.IDENTIFIER) {
            identifiers.add(lexes.next().text());
            if (lexes.is(",")) {
                lexes.next();
                BadSyntax.when(lexes, !lexes.isIdentifier(), "Identifier missing after , in global declaration");
            } else {
                break;
            }

        }
        return identifiers.toArray(String[]::new);
    }
}
