package javax0.genai.pl.analyzer;

import java.util.ArrayList;

public class IdentifierList {
    public static final IdentifierList INSTANCE = new IdentifierList();


    public String[] analyze(final Lex.List lexes) throws BadSyntax {
        final var identifiers = new ArrayList<String>();

        while (lexes.peek().type == Lex.Type.IDENTIFIER) {
            identifiers.add(lexes.next().text);
            if (lexes.is(",")) {
                lexes.next();
                BadSyntax.when(!lexes.isIdentifier(), "Identifier missing after , in global declaration");
            } else {
                break;
            }

        }
        return identifiers.toArray(String[]::new);
    }
}
