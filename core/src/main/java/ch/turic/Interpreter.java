package ch.turic;

import ch.turic.analyzer.Input;
import ch.turic.analyzer.LexList;
import ch.turic.analyzer.Lexer;
import ch.turic.analyzer.ProgramAnalyzer;
import ch.turic.commands.BlockCommand;
import ch.turic.commands.Command;
import ch.turic.memory.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Interprets and executes source code written in the programming language.
 * <p>
 * While this class is designed to be used from a single thread, it implements
 * thread-safe compilation of the source code using double-checked locking pattern.
 * This means that even if multiple threads accidentally execute the same interpreter
 * instance, the source code will be compiled exactly once and the compiled code
 * will be properly visible to all threads.
 * <p>
 * However, note that each execution creates a new {@link Context} instance, which
 * means that concurrent executions will not share state. This is not the recommended
 * usage pattern, and the interpreter should ideally be used from a single thread.
 */
public class Interpreter {
    private final Input source;
    private volatile Command code = null;
    private final Object lock = new Object();
    private Context preprocessorContext;
    private Context ctx;

    public Interpreter(String source) {
        this.source = Input.fromString(source);
    }

    public Interpreter(Input source) {
        this.source = source;
    }

    public ch.turic.Context getImportContext() {
        return ctx;
    }

    /**
     * Executes the source code, compiling it first if necessary.
     * <p>
     * While this method implements thread-safe compilation using double-checked locking,
     * it is not designed for concurrent use. Each call creates a new execution context,
     * so concurrent executions will not share state. It is recommended to use each
     * Interpreter instance from a single thread.
     *
     * @return The result of executing the code
     * @throws BadSyntax          if the source code contains syntax errors
     * @throws ExecutionException if an error occurs during execution
     */
    public Object execute() throws BadSyntax, ExecutionException {
        Command localCode = code; // Read volatile field only once
        if (localCode == null) {
            synchronized (lock) {
                localCode = code; // may have changed since we synchronized
                if (localCode == null) {
                    final var analyzer = new ProgramAnalyzer();
                    LexList lexes = Lexer.analyze(source);
                    if (lexes.isEmpty()) {
                        localCode = new BlockCommand(List.of(), false);
                    } else {
                        localCode = analyzer.analyze(lexes);
                    }
                    code = localCode;
                    preprocessorContext = analyzer.context();
                }
            }
        }

        if (preprocessorContext == null) {
            ctx = new Context();
            BuiltIns.register(ctx);
        } else {
            ctx = preprocessorContext.wrap();
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
