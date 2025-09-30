package ch.turic.cli;

import ch.turic.*;
import ch.turic.analyzer.Lex;
import ch.turic.analyzer.Lexer;
import ch.turic.commands.Closure;
import ch.turic.commands.Macro;
import ch.turic.memory.LocalContext;
import ch.turic.utils.StringNotTerminated;
import ch.turic.utils.UnexpectedCharacter;
import org.jline.reader.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * A command-line Read-Eval-Print Loop (REPL) implementation using JLine for interactive user input.
 * The JLineRepl class provides a console environment where users can interactively evaluate code,
 * manage multiple contexts, and access utilities like help and variable inspection.
 */
public class JLineRepl {

    private enum SyntaxState {
        OK,
        NOT_READY,
        DROP_DEAD
    }

    private static final Pattern SET_COMMAND = Pattern.compile("^\\s*//\\s*set\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(.*)$");

    /**
     * Executes the Turicum REPL (Read-Eval-Print Loop) using JLine for terminal interaction.
     * <p>
     * This method initializes the REPL environment, setting up terminal-based input/output,
     * handling user commands and maintaining execution context. It supports various REPL commands
     * such as input execution, help, listing variables, and exiting the loop. The method processes
     * user input incrementally and supports multi-line code blocks, while managing state transitions.
     * <p>
     * Key features:
     * - Provides a prompt for user input (`>>>` for new input, `...` for continuation).
     * - Maintains a context stack to support block structures within the REPL.
     * - Provides inline help with `//help` and supports context introspection commands (`?`, `??`, `???`).
     * - Manages syntax highlighting and state management for multi-line inputs based on brace matching.
     * - Handles errors gracefully, resetting states in case of syntax or runtime errors.
     *
     * @throws IOException if an input/output error occurs, typically related to terminal interaction
     */
    public static void execute() throws IOException {
        final var interpreter = new Repl();
        final var config = new Config();
        final var console = new Console(interpreter, config);

        final var prompt = ">>> ";
        final var morePrompt = "... ";
        var prefix = "";
        Throwable lastError = null;
        console.output("Turicum REPL with JLine (//help for more info)");

        StringBuilder buffer = new StringBuilder();
        var state = SyntaxState.OK;
        final var ctx_stack = new ArrayList<LocalContext>();

        while (true) {
            String line;
            try {
                final var actualPrompt =
                        switch (state) {
                            case DROP_DEAD -> {
                                console.error("ERROR, INPUT IGNORED\n");
                                yield prefix + prompt;
                            }
                            case OK -> prefix + prompt;
                            case NOT_READY -> prefix + morePrompt;
                        };
                line = console.in(actualPrompt);
            } catch (UserInterruptException | EndOfFileException e) {
                break;
            }

            var cmd = line.trim();

            if (isCommand(cmd, "help")) {
                console.output("""
                        
                        Turicum REPL help screen
                        
                        //exit
                        //! shell_command
                        //. file_name
                        // set variable=value    to configure REPL
                        >>>           is the starting prompt
                        ...           is the continuation prompt
                        {             starts a new context
                        }             closes the current context (the context level is shown at the start of the prompt)
                        //            to drop the current input and start a new one
                        ?             list the local variables (current frame)
                        ??            list all variables
                        ???           list all frames and global variables
                        //help        this help screen
                        """);
                continue;
            }
            if (isCommand(cmd, "exit")) break;
            if (isCommand(cmd, "set")) {
                console.output("REPL CONFIGURATION:");
                for (final var e : config.entrySet()) {
                    console.output(String.format("%s=%s", e.getKey(), e.getValue()));
                }
                console.output("REPL CONFIGURATION KEYS:");
                for (final var e : config.defaultSet()) {
                    console.output(String.format("%s=[%s]", e.getKey(), String.join(", ", e.getValue())));
                }
                state = SyntaxState.OK;
                continue;
            }

            if (cmd.startsWith("//!")) {
                // execute shell script
                final var shell = cmd.substring(3).trim();
                shellExec(shell, console);
                state = SyntaxState.OK;
                continue;
            }
            if (cmd.startsWith("//.")) {
                final var fileName = cmd.substring(3).trim();
                try {
                    console.load(Path.of(fileName));
                    state = SyntaxState.OK;
                    continue;
                } catch (IOException e) {
                    console.error("Error reading file: " + fileName);
                }
            }
            if (cmd.equals("//")) {
                // let the user escape from a long string, ... just drop the current input
                buffer.setLength(0);
                state = SyntaxState.OK;
                continue;
            }
            if (cmd.startsWith("//")) {
                final var matcher = SET_COMMAND.matcher(cmd);
                if (matcher.matches()) {
                    final var key = matcher.group(1);
                    final var value = matcher.group(2);
                    config.set(key, value);

                    state = SyntaxState.OK;
                    continue;
                }
            }
            if (cmd.startsWith("??")) {
                for (int i = cmd.equals("???") ? 0 : 1; i <= ctx_stack.size(); i++) {
                    final var context = i < ctx_stack.size() ? ctx_stack.get(i) : interpreter.ctx;
                    console.output(String.format("Frame #%s:%n", i));
                    for (final var k : context.allFrameKeys()) {
                        printVariable(k, context, console);
                    }
                }
                continue;
            }
            if (line.trim().equals("?")) {
                if (!buffer.isEmpty() && lastError != null) {
                    console.error(lastError.getMessage());
                } else {
                    for (final var k : interpreter.ctx.allFrameKeys()) {
                        printVariable(k, interpreter.ctx, console);
                    }
                }
                continue;
            }
            lastError = null;
            buffer.append(line);
            cmd = buffer.toString();
            if (cmd.matches("\\{+")) {
                state = SyntaxState.OK;
                prefix = openContexts(cmd.length(), ctx_stack, interpreter, buffer);
                continue;
            }
            if (cmd.matches("}+")) {
                if (ctx_stack.isEmpty()) {
                    state = SyntaxState.DROP_DEAD;
                } else {
                    state = SyntaxState.OK;
                    prefix = closeContexts(cmd.length(), interpreter, ctx_stack);
                }
                buffer.setLength(0);
                continue;
            }

            buffer.append("\n");

            state = countBraces(buffer.toString());

            if (state == SyntaxState.DROP_DEAD) {
                buffer.setLength(0);
                continue;
            }

            if (state == SyntaxState.OK && !buffer.isEmpty()) {
                try {
                    var result = interpreter.execute(buffer.toString());
                    if (config.is("command_mode", "on") && result instanceof Closure &&
                            buffer.toString().trim().matches("^\\w+$")) {
                        buffer.append("()");
                        result = interpreter.execute(buffer.toString());
                    }
                    console.output(formatValue(result));
                    console.addToHistory(buffer.toString());
                    // if multi-line, it will be added
                    // if single-line, it is already there, does not alter the history
                    buffer.setLength(0);
                } catch (BadSyntax bs) {
                    if (interpreter.lexes.hasNext()) {
                        console.output(bs.getMessage());
                        buffer.setLength(0);
                        state = SyntaxState.DROP_DEAD;
                        lastError = null;
                    } else {
                        state = SyntaxState.NOT_READY;
                        lastError = bs;
                    }
                } catch (Exception e) {
                    console.error(e.getMessage());
                    buffer.setLength(0);
                    state = SyntaxState.DROP_DEAD;
                }
            }
        }
    }

    /**
     * Executes a shell command and handles its standard output and error streams.
     * The method uses a {@link ProcessBuilder} to start the specified shell command,
     * routing the output and error messages to the provided {@code Console} instance.
     * It also ensures that the output and error streams are handled in separate threads
     * to prevent blocking of the main process.
     *
     * @param shell   the shell command to execute, as a {@code String}
     * @param console the {@code Console} instance to handle output and error messages
     */
    private static void shellExec(String shell, Console console) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                processBuilder.command("cmd", "/c", shell);
            } else {
                processBuilder.command("sh", "-c", shell);
            }

            Process process = processBuilder.start();

            // Read output and error streams in separate threads to prevent blocking
            Thread outputThread = new Thread(() -> {
                try (var reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        console.shell_output(line);
                    }
                } catch (IOException e) {
                    console.error("Error reading process output: " + e.getMessage());
                }
            });

            Thread errorThread = new Thread(() -> {
                try (var reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        console.error(line);
                    }
                } catch (IOException e) {
                    console.error("Error reading process error stream: " + e.getMessage());
                }
            });

            outputThread.start();
            errorThread.start();

            // Wait for the process to complete
            int exitCode = process.waitFor();
            outputThread.join();
            errorThread.join();

            if (exitCode != 0) {
                console.error("Process exited with code " + exitCode);
            }
        } catch (IOException e) {
            console.error("Error executing shell command: " + e.getMessage());
        } catch (InterruptedException e) {
            console.error("Process execution interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private static String openContexts(int num, ArrayList<LocalContext> ctx_stack, Repl interpreter, StringBuilder buffer) {
        String prefix;
        for (int i = 0; i < num; i++) {
            ctx_stack.add(interpreter.ctx);
            interpreter.ctx = interpreter.ctx.wrap();
        }
        prefix = calculatePrefix(ctx_stack);
        buffer.setLength(0);
        return prefix;
    }

    private static String closeContexts(int num, Repl interpreter, ArrayList<LocalContext> ctx_stack) {
        String prefix;
        for (int i = 0; i < num; i++) {
            interpreter.ctx = ctx_stack.removeLast();
        }
        prefix = calculatePrefix(ctx_stack);
        return prefix;
    }

    private static String calculatePrefix(ArrayList<LocalContext> ctx_stack) {
        final var num = ctx_stack.size();
        if (num > 5) {
            return "{" + (num) + "{";
        } else {
            return "{".repeat(num);
        }
    }

    private static void printVariable(String k, LocalContext context, Console console) {
        Object value = formatValue(context.get(k));
        console.output(String.format("%s: %s", k, value));
    }

    private static String formatValue(Object value) {
        return switch (value) {
            case TuriMacro ignored -> "built-in";
            case TuriFunction ignored -> "built-in";
            case Closure ignored -> "fn()";
            case Macro ignored -> "macro fn()";
            case null -> "none";
            default -> value.toString();
        };
    }

    private static SyntaxState countBraces(String s) {
        try {
            final var lexes = Lexer.analyze(Input.fromString(s));
            final var braces = new ArrayList<Lex>();
            while (lexes.hasNext()) {
                if (lexes.is("{", "(", "[")) {
                    braces.add(lexes.next());
                } else if (lexes.is("}", ")", "]")) {
                    if (braces.isEmpty()) {
                        return SyntaxState.DROP_DEAD;
                    }
                    final var lastB = braces.removeLast();
                    if ((lexes.is("}") && !lastB.text().equals("{")) ||
                            (lexes.is(")") && !lastB.text().equals("(")) ||
                            (lexes.is("]") && !lastB.text().equals("["))) {
                        return SyntaxState.DROP_DEAD;
                    }
                    lexes.next();
                } else {
                    lexes.next();
                }
            }
            return braces.isEmpty() ? SyntaxState.OK : SyntaxState.NOT_READY;
        } catch (StringNotTerminated | UnexpectedCharacter snt) {
            return SyntaxState.DROP_DEAD;// we will see how to handle this
        } catch (BadSyntax e) {
            return SyntaxState.NOT_READY;
        }
    }

    private static boolean isCommand(String line, String command) {
        return line.startsWith("//") && line.endsWith(command) && line.substring(2, line.length() - command.length()).isBlank();
    }
}
