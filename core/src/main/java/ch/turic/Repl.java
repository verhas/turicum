package ch.turic;

import ch.turic.analyzer.Input;
import ch.turic.analyzer.LexList;
import ch.turic.analyzer.Lexer;
import ch.turic.analyzer.ProgramAnalyzer;
import ch.turic.commands.BlockCommand;
import ch.turic.commands.Command;
import ch.turic.memory.Context;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A repl interpreter executes a small piece of code, and then it can be reinvoked with ever newer pieces of codes.
 * <p>
 * It cannot be embedded in a multi-thread environment.
 */
public class Repl {

    public Context ctx;
    public LexList lexes;

    public Repl() {
        ctx = new Context();
        BuiltIns.register(ctx);
    }

    public Collection<String> completions() {
        final var result = new ArrayList<String>();
        result.addAll(Lexer.RESERVED);
        result.addAll(ctx.allKeys());
        return result;
    }

    public Object execute(String source) throws BadSyntax, ExecutionException {
        Command localCode;
        final var analyzer = new ProgramAnalyzer();
        lexes = Lexer.analyze(Input.fromString(source));
        if (lexes.isEmpty()) {
            localCode = new BlockCommand(new Command[0], false);
        } else {
            localCode = analyzer.analyze(lexes);
        }
        try {
            return localCode.execute(ctx);
        } catch (ExecutionException e) {
            final var newStackTrace = new ArrayList<StackTraceElement>();
            for (final var stackFrame : ctx.threadContext.getStackTrace()) {
                if (stackFrame.command().startPosition() != null) {
                    newStackTrace.add(new StackTraceElement(
                            stackFrame.command().getClass().getSimpleName(),
                            "",
                            stackFrame.command().startPosition().file,
                            stackFrame.command().startPosition().line
                    ));
                }
            }
            final var turiException = new ExecutionException(e);
            turiException.setStackTrace(newStackTrace.toArray(StackTraceElement[]::new));
            throw turiException;
        }
    }
}
