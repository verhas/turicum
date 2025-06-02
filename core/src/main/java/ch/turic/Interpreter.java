package ch.turic;

import ch.turic.analyzer.LexList;
import ch.turic.analyzer.Lexer;
import ch.turic.analyzer.ProgramAnalyzer;
import ch.turic.memory.Context;
import ch.turic.utils.Marshaller;
import ch.turic.utils.Unmarshaller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

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
    private ch.turic.analyzer.Input source = null;
    private volatile Program code = null;
    private final Object lock = new Object();
    private Context preprocessorContext;
    private Context ctx;

    public Interpreter(String source) {
        this.source = (ch.turic.analyzer.Input) ch.turic.Input.fromString(source);
    }

    /**
     * Constructs an Interpreter instance with the specified input source.
     *
     * @param source the input source to be interpreted
     */
    public Interpreter(Input source) {
        this.source = (ch.turic.analyzer.Input) source;
    }

    /**
     * Constructs an Interpreter instance with the specified command to be executed.
     *
     * @param code the command to be interpreted and executed
     */
    public Interpreter(Program code) {
        this.code = code;
    }

    /**
     * Constructs an {@code Interpreter} instance based on the given file. Depending on the file extension,
     * creates an interpreter for source code files ending with ".turi" or deserializes a compiled program
     * from files ending with ".turc".
     *
     * @param file the path to the source or compiled program file
     * @throws IOException if an I/O error occurs while reading the file
     * @throws RuntimeException if the file type is unsupported
     */
    public Interpreter(Path file) throws IOException {
        String fn = file.toFile().getAbsolutePath();
        if (fn.endsWith(".turi")) {
            final var s = Files.readString(file, StandardCharsets.UTF_8);
            this.source = new ch.turic.analyzer.Input(new StringBuilder(s), fn);
        } else if (fn.endsWith(".turc")) {
            final var bytes = Files.readAllBytes(file);
            final var unmarshaller = new Unmarshaller();
            this.code = unmarshaller.deserialize(bytes);
            this.ctx = new Context();
            BuiltIns.register(ctx);
        } else {
            throw new RuntimeException("Unsupported file type");
        }
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
    public Object compileAndExecute() throws BadSyntax, ExecutionException {
        Command localCode = compile();
        return execute(localCode);
    }

    /**
     * Executes the provided command within the current execution context.
     * If an execution error occurs, it adapts the stack trace to include
     * source-level information before re-throwing the exception.
     *
     * @param code the command to execute
     * @return the result of executing the command
     * @throws ExecutionException if an error occurs during the command execution,
     *                            with an adapted stack trace for better debugging
     */
    public Object execute(Command code) {
        try {
            return code.execute(ctx);
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

    /**
     * Compiles the source code into an executable {@link Program} object.
     * This method uses double-checked locking to ensure thread-safe compilation.
     * If the code has already been compiled, the cached {@link Program} is returned.
     * Otherwise, the source code is lexically analyzed and processed into a series of commands.
     *
     * @return the compiled {@link Program} object representing the executable commands
     */
    public Program compile() {
        Command localCode = code; // Read volatile field only once
        if (localCode == null) {
            synchronized (lock) {
                localCode = code; // may have changed since we synchronized
                if (localCode == null) {
                    final var analyzer = new ProgramAnalyzer();
                    LexList lexes = Lexer.analyze(source);
                    if (lexes.isEmpty()) {
                        localCode = new Program(new Command[0]);
                    } else {
                        localCode = analyzer.analyze(lexes);
                    }
                    code = (Program) localCode;
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
        return (Program) localCode;
    }

    public byte[] serialize() {
        final var marshaller = new Marshaller();
        return marshaller.serialize((Program) code);
    }
}
