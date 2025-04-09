package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import javax0.turicum.BuiltIns;
import javax0.turicum.ExecutionException;
import javax0.turicum.commands.Closure;
import javax0.turicum.commands.Command;
import javax0.turicum.commands.Program;
import javax0.turicum.memory.Context;

import java.util.ArrayList;

public class ProgramAnalyzer implements Analyzer {
    private Context preprocessorContext = null;

    @Override
    public Command analyze(LexList lexes) throws BadSyntax {
        final var commands = new ArrayList<Command>();
        while (lexes.hasNext()) {
            if (lexes.is("#")) {
                lexes.next();
                final var preprocessor = CommandAnalyzer.INSTANCE.analyze(lexes);
                if (preprocessorContext == null) {
                    preprocessorContext = new Context();
                    BuiltIns.register(preprocessorContext);
                }
                preprocess(lexes, preprocessor, commands);
            }
            final var cmd = CommandAnalyzer.INSTANCE.analyze(lexes);
            if (cmd != null) {
                commands.add(cmd);
            }
        }
        return new Program(commands.toArray(Command[]::new));
    }

    public Context context(){
        return preprocessorContext;
    }

    private void preprocess(LexList lexes, Command preprocessor, ArrayList<Command> commands) {
        lexes.purge();
        new Program(commands.toArray(Command[]::new)).execute(preprocessorContext);
        final var preprocClosure = preprocessor.execute(preprocessorContext);
        if (preprocClosure instanceof Closure closure) {
            final var newLexes = closure.call(preprocessorContext, lexes);
            lexes.array.clear();
            if (newLexes instanceof Iterable<?> lngListLexes) {
                lngListLexes.forEach(item -> {
                    if (item instanceof Lex lex) {
                        lexes.array.add(lex);
                    } else {
                        lexes.array.addAll(Lexer.analyze(Input.fromString(item.toString())).array);
                    }
                });
            } else {
                throw new ExecutionException("Preprocessor returned '%s', not usable", newLexes);
            }
            commands.clear();
        } else {
            throw new ExecutionException("Preprocessor is not executable");
        }
    }
}
