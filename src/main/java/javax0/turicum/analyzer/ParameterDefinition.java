package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import javax0.turicum.commands.ParameterList;

import java.util.ArrayList;

public class ParameterDefinition {
    public static final ParameterDefinition INSTANCE = new ParameterDefinition();


    public ParameterList analyze(final Lex.List lexes) throws BadSyntax {
        final var identifiers = new ArrayList<String>();

        while (lexes.peek().type()== Lex.Type.IDENTIFIER) {
            identifiers.add(lexes.next().text());
            if (lexes.is(",")) {
                lexes.next();
                BadSyntax.when(!lexes.isIdentifier(), "Identifier missing after , in global declaration");
            } else {
                break;
            }

        }
        return new ParameterList(identifiers.stream()
                .map(id -> new ParameterList.Parameter(id, ParameterList.Parameter.Type.POSITIONAL_ONLY,null,null))
                .toArray(ParameterList.Parameter[]::new),null,null,null);
    }
}
