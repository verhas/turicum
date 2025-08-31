package ch.turic.cli;

import ch.turic.*;
import ch.turic.analyzer.Lex;
import ch.turic.analyzer.Lexer;
import ch.turic.commands.Closure;
import ch.turic.commands.Macro;
import ch.turic.memory.Context;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.ArrayList;

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

    /**
     * Executes the Turicum REPL (Read-Eval-Print Loop) using JLine for terminal interaction.
     * <p>
     * This method initializes the REPL environment, setting up terminal-based input/output,
     * handling user commands, and maintaining execution context. It supports various REPL commands
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
        Terminal terminal = TerminalBuilder.builder().system(true).build();
        Parser parser = new DefaultParser();
        final var history = new DefaultHistory();
        LineReader reader = LineReaderBuilder.builder()
                .appName("Turicum")
                .terminal(terminal)
                .completer(new StringsCompleter(interpreter::completions))
                .parser(parser)
                .history(history)
                .build();

        final var prompt = ">>> ";
        final var morePrompt = "... ";
        var prefix = "";
        Throwable lastError = null;
        System.out.println("Turicum REPL with JLine (//help for more info)");

        StringBuilder buffer = new StringBuilder();
        var state = SyntaxState.OK;
        final var ctx_stack = new ArrayList<Context>();

        while (true) {
            String line;
            try {
                final var actualPrompt =
                        switch (state) {
                            case DROP_DEAD -> "ERROR, INPUT IGNORED\n" + prefix + prompt;
                            case OK -> prefix + prompt;
                            case NOT_READY -> prefix + morePrompt;
                        };
                line = reader.readLine(actualPrompt);
            } catch (UserInterruptException | EndOfFileException e) {
                break;
            }

            var cmd = line.trim();

            if (cmd.startsWith("//") && cmd.endsWith("help") && cmd.substring(2, cmd.length() - 4).isBlank()) {
                System.out.println("""
                        
                        Turicum REPL help screen
                        
                        //exit to exit the repl
                        
                        >>>    is the starting prompt
                        ...    is the continuation prompt
                        {      starts a new context
                        }      closes the current context (the context level is shown at the start of the prompt)
                        //     to drop the current input and start a new one
                        ?      list the local variables (current frame)
                        ??     list all variables
                        ???    list all frames and global variables
                        //help this help screen
                        """);
                continue;
            }
            if (cmd.startsWith("//") && cmd.endsWith("exit") && cmd.substring(2, cmd.length() - 4).isBlank()) break;
            if (cmd.equals("//")) {
                buffer.setLength(0);
                state = SyntaxState.OK;
                continue;
            }
            if (cmd.startsWith("??")) {
                for (int i = cmd.equals("???") ? 0 : 1; i <= ctx_stack.size(); i++) {
                    final var context = i < ctx_stack.size() ? ctx_stack.get(i) : interpreter.ctx;
                    System.out.printf("Frame #%s:%n", i);
                    for (final var k : context.allFrameKeys()) {
                        printVariable(k, context);
                    }
                }
                continue;
            }
            if (line.trim().equals("?")) {
                if (!buffer.isEmpty() && lastError != null) {
                    System.out.println(lastError.getMessage());
                } else {
                    for (final var k : interpreter.ctx.allFrameKeys()) {
                        printVariable(k, interpreter.ctx);
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
                    System.out.println(
                            formatValue(interpreter.execute(buffer.toString())));
                    history.add(buffer.toString());
                    // if multi-line it will be added
                    // if single line, it is already there, does not alter the history
                    buffer.setLength(0);
                } catch (BadSyntax bs) {
                    if (interpreter.lexes.hasNext()) {
                        System.out.println(bs.getMessage());
                        buffer.setLength(0);
                        state = SyntaxState.DROP_DEAD;
                        lastError = null;
                    } else {
                        state = SyntaxState.NOT_READY;
                        lastError = bs;
                    }
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    buffer.setLength(0);
                    state = SyntaxState.DROP_DEAD;
                }
            }
        }
    }

    private static String openContexts(int num, ArrayList<Context> ctx_stack, Repl interpreter, StringBuilder buffer) {
        String prefix;
        for (int i = 0; i < num; i++) {
            ctx_stack.add(interpreter.ctx);
            interpreter.ctx = interpreter.ctx.wrap();
        }
        prefix = calculatePrefix(ctx_stack);
        buffer.setLength(0);
        return prefix;
    }

    private static String closeContexts(int num, Repl interpreter, ArrayList<Context> ctx_stack) {
        String prefix;
        for (int i = 0; i < num; i++) {
            interpreter.ctx = ctx_stack.removeLast();
        }
        prefix = calculatePrefix(ctx_stack);
        return prefix;
    }

    private static String calculatePrefix(ArrayList<Context> ctx_stack) {
        final var num = ctx_stack.size();
        if (num > 5) {
            return "{" + (num) + "{";
        } else {
            return "{".repeat(num);
        }
    }

    private static void printVariable(String k, Context context) {
        Object value = formatValue(context.get(k));
        System.out.printf("%s: %s\n", k, value);
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
        } catch (BadSyntax e) {
            return SyntaxState.NOT_READY;// we will see how to handle this
        }
    }
}
