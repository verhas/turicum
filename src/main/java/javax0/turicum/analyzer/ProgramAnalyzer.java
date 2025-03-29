package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import javax0.turicum.commands.Command;
import javax0.turicum.commands.Program;

import java.util.ArrayList;

public class ProgramAnalyzer implements Analyzer {
    public static final ProgramAnalyzer INSTANCE = new ProgramAnalyzer();

    @Override
    public Command analyze(Lex.List lexes) throws BadSyntax {
        final var list = new ArrayList<Command>();
        while (lexes.hasNext()) {
            final var cmd = CommandAnalyzer.INSTANCE.analyze(lexes);
            if( cmd != null ) {
                list.add(cmd);
            }
        }
        return new Program(list.toArray(Command[]::new));
    }
}
