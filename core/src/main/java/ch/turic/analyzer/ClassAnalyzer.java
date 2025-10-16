package ch.turic.analyzer;

import ch.turic.exceptions.BadSyntax;
import ch.turic.commands.BlockCommand;
import ch.turic.commands.ClassDefinition;
import ch.turic.Command;

/**
 * The {@code ClassAnalyzer} class is responsible for parsing and analyzing class definitions
 * from a sequence of lexemes. It extends the {@code AbstractAnalyzer} class and provides
 * an implementation of the {@code _analyze} method to process the class syntax.
 * <p>
 * Class definitions in this context may include:
 * - An optional class name.
 * - An optional list of parent class names following the ":" symbol.
 * - A block of commands defining the body of the class.
 * <p><pre>
 * snippet EBNF_CLASS
 * CLASS ::= 'class' [IDENTIFIER] [INHERITANCE] BODY
 *
 * INHERITANCE ::= ':' PARENT_LIST
 *
 * PARENT_LIST ::= IDENTIFIER (',' IDENTIFIER)*
 *
 * BODY ::= '{' COMMAND* '}'
 *
 * end snippet
 * </pre>
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
        final String[] parents;
        if (lexes.is(":")) {
            lexes.next();
            parents = IdentifierList.INSTANCE.analyze(lexes);
            BadSyntax.when(lexes, parents.length == 0, "The list of the parents must not be empty following the ':'. Just leave the ':'.");
        } else {
            parents = null;
        }
        final var block = BlockAnalyzer.UNWRAPPED.analyze(lexes);
        return new ClassDefinition(cn, parents, (BlockCommand) block);
    }
}
