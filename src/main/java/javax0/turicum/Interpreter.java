package javax0.turicum;

import javax0.turicum.analyzer.Input;
import javax0.turicum.analyzer.Lexer;
import javax0.turicum.analyzer.ProgramAnalyzer;
import javax0.turicum.commands.Command;
import javax0.turicum.memory.Context;

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
    private final String source;
    private volatile Command code = null;
    private final Object lock = new Object();
    private Context preprocessorContext;

    public Interpreter(String source) {
        this.source = source;
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
                localCode = code; // may have changed since we syncronized
                if (localCode == null) {
                    final var analyzer = new ProgramAnalyzer();
                    localCode = analyzer.analyze(Lexer.analyze(Input.fromString(source)));
                    code = localCode;
                    preprocessorContext = analyzer.context();
                }
            }
        }

        final Context ctx;
        if (preprocessorContext == null) {
            ctx = new Context();
            BuiltIns.register(ctx);
        } else {
            ctx = preprocessorContext.wrap();
        }
        return localCode.execute(ctx);
    }
}
