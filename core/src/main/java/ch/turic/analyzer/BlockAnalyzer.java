package ch.turic.analyzer;


import ch.turic.exceptions.BadSyntax;
import ch.turic.commands.BlockCommand;
import ch.turic.Command;

import java.util.ArrayList;

public class BlockAnalyzer extends AbstractAnalyzer {
    public static final BlockAnalyzer INSTANCE = new BlockAnalyzer("}", true);
    public static final BlockAnalyzer UNWRAPPED = new BlockAnalyzer("}", false);
    public static final BlockAnalyzer FLAT = new BlockAnalyzer(")", false);

    final String closing;
    final boolean wrap;

    public boolean wrap() {
        return wrap;
    }

    private BlockAnalyzer(String closing, boolean wrap) {
        this.closing = closing;
        this.wrap = wrap;
    }

    @Override
    public BlockCommand _analyze(final LexList lexes) throws BadSyntax {
        lexes.next();
        final var commands = getCommands(lexes);
        return new BlockCommand(commands.toArray(Command[]::new), wrap);
    }

    ArrayList<Command> getCommands(LexList lexes) throws BadSyntax {
        final var commands = new ArrayList<Command>();
        while( lexes.isNot(closing)){
            final var cmd = CommandAnalyzer.INSTANCE.analyze(lexes);
            if( cmd != null ) {
                commands.add(cmd);
            }
        }
        lexes.next(); // eat the closing '}'
        return commands;
    }
}
