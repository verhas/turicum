package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.commands.BlockCommand;
import ch.turic.commands.ClassDefinition;
import ch.turic.commands.Command;

/**
 * <pre>{@code
 * class myClass myClass(parameters) : a,b,c,d {
 *     commands
 * }
 * }</pre>
 * }
 * <p>
 * Creates a ClassDefinition.
 * <p>
 * {@code parameters} with the surrounding '(' and ')' are optional
 * <p>
 * Class name is optional.
 * <p>
 * '{@code : a,b,c,d}' parent classes definitions are optional.
 */
public class ClassAnalyzer extends AbstractAnalyzer {
    public static final ClassAnalyzer INSTANCE = new ClassAnalyzer();

    @Override
    public Command _analyze(LexList lexes) throws BadSyntax {
        final String cn;
        if (lexes.isIdentifier()) {
            cn = lexes.next().text();
        } else {
            cn = null;
        }
        String[] parameters;
        if (lexes.is("(")) {
            lexes.next();
            parameters = IdentifierList.INSTANCE.analyze(lexes);
            BadSyntax.when(lexes, !lexes.is(")"), "Constructor parameters should be followed by ')'");
            lexes.next();
        } else {
            parameters = null;
        }
        final String[] parents;
        if (lexes.is(":")) {
            lexes.next();
            parents = IdentifierList.INSTANCE.analyze(lexes);
            BadSyntax.when(lexes, parents.length == 0, "The list of the parents must not be empty following the ':'. Just leave the ':'.");
        } else {
            parents = null;
        }
        final var block = BlockAnalyzer.UNWRAPPED.analyze(lexes);
        return new ClassDefinition(cn, parents, parameters, (BlockCommand) block);
    }
}
