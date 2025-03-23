package javax0.genai.pl.analyzer;

import javax0.genai.pl.commands.Command;
import javax0.genai.pl.commands.Program;

import java.util.ArrayList;

public class ProgramAnalyzer implements Analyzer {
    public static final ProgramAnalyzer INSTANCE = new ProgramAnalyzer();

    @Override
    public Command analyze(Lex.List lexes) throws BadSyntax {
        final var list = new ArrayList<Command>();
        while (lexes.hasNext()) {
            list.add(CommandAnalyzer.INSTANCE.analyze(lexes));
        }
        return new Program(list.toArray(Command[]::new));
    }
}
