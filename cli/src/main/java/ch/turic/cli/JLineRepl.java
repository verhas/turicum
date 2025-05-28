package ch.turic.cli;

import ch.turic.BadSyntax;
import ch.turic.Repl;
import ch.turic.TuriFunction;
import ch.turic.TuriMacro;
import ch.turic.analyzer.Input;
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
import java.util.Objects;

public class JLineRepl {
    public static void execute() throws IOException {
        final var interpreter = new Repl();
        Terminal terminal = TerminalBuilder.builder().system(true).build();
        Parser parser = new DefaultParser();
        LineReader reader = LineReaderBuilder.builder()
                .appName("Turicum")
                .terminal(terminal)
                .completer(new StringsCompleter(interpreter::completions))
                .parser(parser)
                .history(new DefaultHistory())
                .build();

        String prompt = ">>> ";
        String morePrompt = "... ";

        System.out.println("Turicum REPL with JLine (//help for more info)");

        StringBuilder buffer = new StringBuilder();
        int openBraces = 0;
        final var ctx_stack = new ArrayList<Context>();

        while (true) {
            String line;
            try {
                final var actualPrompt =
                        switch (openBraces) {
                            case -1 -> "ERROR, INPUT IGNORED\n" + prompt;
                            case 0 -> prompt;
                            default -> morePrompt;
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
                        }      closes the current context (the context level is show at the start of the prompt)
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
                openBraces = 0;
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
                openBraces = 0;
                continue;
            }
            if (line.trim().equals("?")) {
                for (final var k : interpreter.ctx.allFrameKeys()) {
                    printVariable(k, interpreter.ctx);
                }
                openBraces = 0;
                continue;
            }
            buffer.append(line);
            cmd = buffer.toString();
            if (cmd.matches("\\{+")) {
                openBraces = 0;
                prompt = "{".repeat(cmd.length()) + prompt;
                for (int i = 0; i < cmd.length(); i++) {
                    ctx_stack.add(interpreter.ctx);
                    interpreter.ctx = interpreter.ctx.wrap();
                }
                buffer.setLength(0);
                continue;
            }
            if (cmd.matches("}+")) {
                openBraces = 0;
                if (ctx_stack.isEmpty()) {
                    openBraces = -1;
                } else {
                    prompt = prompt.substring(cmd.length());
                    for (int i = 0; i < cmd.length(); i++) {
                        interpreter.ctx = ctx_stack.removeLast();
                    }
                }
                buffer.setLength(0);
                continue;
            }

            buffer.append("\n");

            openBraces = countBraces(buffer.toString());

            if (openBraces == -1) {
                buffer.setLength(0);
                continue;
            }

            if (openBraces == 0 && !buffer.isEmpty()) {
                String input = buffer.toString();
                buffer.setLength(0);
                try {
                    System.out.println(Objects.requireNonNullElse(interpreter.execute(input), "none"));
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    private static void printVariable(String k, Context context) {
        Object value = Objects.requireNonNullElse(context.get(k), "none");
        value = switch (value) {
            case TuriMacro ignored -> "built-in";
            case TuriFunction ignored -> "built-in";
            case Closure ignored -> "fn()";
            case Macro ignored -> "macro fn()";
            default -> value;
        };
        System.out.printf("%s: %s\n", k, value);
    }

    private static int countBraces(String s) {
        try {
            final var lexes = Lexer.analyze(Input.fromString(s));
            final var braces = new ArrayList<Lex>();
            while (lexes.hasNext()) {
                if (lexes.is("{", "(", "[")) {
                    braces.add(lexes.next());
                } else if (lexes.is("}", ")", "]")) {
                    if (braces.isEmpty()) {
                        return -1;
                    }
                    final var lastB = braces.removeLast();
                    if ((lexes.is("}") && !lastB.text().equals("{")) ||
                            (lexes.is(")") && !lastB.text().equals("(")) ||
                            (lexes.is("]") && !lastB.text().equals("["))) {
                        return -1;
                    }
                    lexes.next();
                } else {
                    lexes.next();
                }
            }
            return braces.size();
        } catch (BadSyntax e) {
            return 1;// we will see how to handle this
        }
    }
}
