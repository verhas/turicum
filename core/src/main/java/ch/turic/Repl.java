package ch.turic;

import ch.turic.analyzer.LexList;
import ch.turic.analyzer.Lexer;
import ch.turic.analyzer.ProgramAnalyzer;
import ch.turic.commands.BlockCommand;
import ch.turic.memory.Context;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A REPL (Read-Eval-Print Loop) interpreter that executes code incrementally.
 * This interpreter can process and execute small pieces of code sequentially,
 * maintaining state between executions.
 * <p>
 * The interpreter manages its own context and provides support for code completion
 * through the {@link #completions()} method. It handles lexical analysis, program
 * analysis, and execution of the provided code.
 * <p>
 * Thread Safety: This class is not thread-safe and should not be used in a
 * multi-threaded environment. Each instance maintains mutable state that could be
 * corrupted by concurrent access.
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

    /**
     * Executes the provided source code and returns the result of the execution.
     *
     * @param source The source code to execute as a string
     * @return The result of executing the code, which can be any Object
     * @throws BadSyntax          if the provided code contains syntax errors
     * @throws ExecutionException if an error occurs during code execution
     */
    public Object execute(String source) throws BadSyntax, ExecutionException {
        Command localCode;
        final var analyzer = new ProgramAnalyzer();
        lexes = Lexer.analyze((ch.turic.analyzer.Input)Input.fromString(source));
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
