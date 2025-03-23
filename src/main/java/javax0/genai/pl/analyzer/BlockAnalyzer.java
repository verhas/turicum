package javax0.genai.pl.analyzer;


import javax0.genai.pl.commands.BlockCommand;
import javax0.genai.pl.commands.Command;

import java.util.ArrayList;

public record BlockAnalyzer(boolean wrap) implements Analyzer {
    public static final BlockAnalyzer INSTANCE = new BlockAnalyzer(true);
    public static final BlockAnalyzer UNWRAPPED = new BlockAnalyzer(false);

    @Override
    public BlockCommand analyze(final Lex.List lexes) throws BadSyntax {
        lexes.next();
        final var commands = new ArrayList<Command>();
        while( lexes.isNot("}")){
            commands.add(CommandAnalyzer.INSTANCE.analyze(lexes));
        }
        lexes.next(); // eat the closing '}'
        return new BlockCommand(commands, wrap);
    }
}
