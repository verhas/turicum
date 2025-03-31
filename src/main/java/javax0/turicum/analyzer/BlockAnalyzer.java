package javax0.turicum.analyzer;


import javax0.turicum.BadSyntax;
import javax0.turicum.commands.BlockCommand;
import javax0.turicum.commands.Command;

import java.util.ArrayList;

public record BlockAnalyzer(boolean wrap) implements Analyzer {
    public static final BlockAnalyzer INSTANCE = new BlockAnalyzer(true);
    public static final BlockAnalyzer UNWRAPPED = new BlockAnalyzer(false);

    @Override
    public BlockCommand analyze(final Lex.List lexes) throws BadSyntax {
        lexes.next();
        final var commands = getCommands(lexes);
        return new BlockCommand(commands, wrap);
    }

    static ArrayList<Command> getCommands(Lex.List lexes) throws BadSyntax {
        final var commands = new ArrayList<Command>();
        while (lexes.isNot("}")) {
            final var cmd = CommandAnalyzer.INSTANCE.analyze(lexes);
            if (cmd != null) {
                commands.add(cmd);
            }
        }
        lexes.next(); // eat the closing '}'
        return commands;
    }

}
