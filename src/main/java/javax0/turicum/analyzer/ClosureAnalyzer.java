package javax0.turicum.analyzer;

import javax0.turicum.commands.BlockCommand;
import javax0.turicum.commands.ClosureDefinition;
import javax0.turicum.commands.Command;

/**
 * <pre>{@code
 * { |a,b,c,d| commands }
 * }</pre>
 */
public class ClosureAnalyzer implements Analyzer {
    public static final ClosureAnalyzer INSTANCE = new ClosureAnalyzer();

    @Override
    public Command analyze(Lex.List lexes) throws BadSyntax {
        final var identifiers = IdentifierList.INSTANCE.analyze(lexes);
        lexes.peek(Lex.Type.RESERVED, "|", "Closure arguments but be between two '|' characters");
        lexes.next();
        final var commands = BlockAnalyzer.getCommands(lexes);
        final var block = new BlockCommand(commands, true);
        return new ClosureDefinition(identifiers, block);
    }

}
