package ch.turic.analyzer;


import ch.turic.BadSyntax;
import ch.turic.commands.BlockCommand;
import ch.turic.commands.Command;

import java.util.ArrayList;

public class BlockAnalyzer extends AbstractAnalyzer {
    public static final BlockAnalyzer INSTANCE = new BlockAnalyzer(true);
    public static final BlockAnalyzer UNWRAPPED = new BlockAnalyzer(false);

    final boolean wrap;

    public boolean wrap() {
        return wrap;
    }

    public BlockAnalyzer(boolean wrap) {
        this.wrap = wrap;
    }

    @Override
    public BlockCommand _analyze(final LexList lexes) throws BadSyntax {
        lexes.next();
        final var commands = getCommands(lexes);
        return new BlockCommand(commands, wrap);
    }

    static ArrayList<Command> getCommands(LexList lexes) throws BadSyntax {
        final var commands = new ArrayList<Command>();
        while( lexes.isNot("}")){
            final var cmd = CommandAnalyzer.INSTANCE.analyze(lexes);
            if( cmd != null ) {
                commands.add(cmd);
            }
        }
        lexes.next(); // eat the closing '}'
        return commands;
    }
}
