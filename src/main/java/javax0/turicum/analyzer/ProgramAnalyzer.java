package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import javax0.turicum.BuiltIns;
import javax0.turicum.commands.Closure;
import javax0.turicum.commands.Command;
import javax0.turicum.commands.Program;
import javax0.turicum.memory.Context;

import java.util.ArrayList;

public class ProgramAnalyzer implements Analyzer {
    public static final ProgramAnalyzer INSTANCE = new ProgramAnalyzer();

    @Override
    public Command analyze(LexList lexes) throws BadSyntax {
        final var list = new ArrayList<Command>();
        while (lexes.hasNext()) {
            if (lexes.is("#")) {
                lexes.next();
                final var preprocessor = CommandAnalyzer.INSTANCE.analyze(lexes);
                if (preprocessor instanceof Closure closure) {
                    if( closure.parameters().parameters().length != 1 ){
                        throw new BadSyntax("Preprocessor closure must have one single parameter");
                    }
                    final var ctx = new Context();
                    BuiltIns.register(ctx);
                    new Program(list.toArray(Command[]::new)).execute(ctx);
                    list.clear();

                } else {
                    throw new BadSyntax("Preprocessing command must be a closure");
                }

            }
            final var cmd = CommandAnalyzer.INSTANCE.analyze(lexes);
            if (cmd != null) {
                list.add(cmd);
            }
        }
        return new Program(list.toArray(Command[]::new));
    }
}
