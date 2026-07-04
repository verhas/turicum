package ch.turic.analyzer;

import ch.turic.Command;
import ch.turic.commands.Identifier;
import ch.turic.commands.Veil;
import ch.turic.exceptions.BadSyntax;

import java.util.ArrayList;

/**
 * Analyzes the {@code veil} command: the keyword followed by a comma-separated, non-empty list
 * of identifiers, e.g. {@code veil balance, helper}.
 */
public class VeilAnalyzer extends AbstractAnalyzer {
    public static final VeilAnalyzer INSTANCE = new VeilAnalyzer();

    @Override
    public Command _analyze(LexList lexes) throws BadSyntax {
        final var identifiers = new ArrayList<Identifier>();
        while (lexes.isIdentifier()) {
            identifiers.add(new Identifier(lexes.next().text()));
            if (lexes.isNot(",")) {
                break;
            }
            lexes.next();
        }
        BadSyntax.when(lexes, identifiers.isEmpty(), "Identifier expected after 'veil'");
        return new Veil(identifiers.toArray(Identifier[]::new));
    }
}
