package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.BuiltIns;
import ch.turic.ExecutionException;
import ch.turic.Input;
import ch.turic.commands.Closure;
import ch.turic.Command;
import ch.turic.Program;
import ch.turic.memory.Context;

import java.util.ArrayList;

public class ProgramAnalyzer extends AbstractAnalyzer {
    private Context preprocessorContext = null;

    @Override
    public Command _analyze(LexList lexes) throws BadSyntax {
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
                        lexes.array.addAll(Lexer.analyze((ch.turic.analyzer.Input)Input.fromString(item.toString())).array);
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
