package ch.turic.commands;


import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.Input;
import ch.turic.analyzer.BlockAnalyzer;
import ch.turic.analyzer.Lex;
import ch.turic.analyzer.Lexer;
import ch.turic.memory.Context;
import ch.turic.utils.Unmarshaller;

import java.util.ArrayList;
import java.util.Objects;

public class StringConstant extends AbstractCommand {
    final private String value;
    final private Command[] commands;

    public static StringConstant factory(Unmarshaller.Args args) {
        return new StringConstant(args.str("value"),
                args.commands());
    }

    public String value() {
        return value;
    }

    private StringConstant(final String value, final Command[] commands) {
        this.value = value;
        this.commands = commands;
    }

    public StringConstant(final Lex lex) {
        this(lex.text(), lex.interpolated);
    }
    public StringConstant(final String value, final boolean interpolated) {
        Objects.requireNonNull(value);
        if (interpolated) {
            final var parts = split(value);
            commands = new Command[parts.length];
            for (int i = 0; i < parts.length; i += 2) {
                commands[i] = new StringConstant(parts[i], false);
            }
            for (int i = 1; i < parts.length; i += 2) {
                final var lexes = Lexer.analyze((ch.turic.analyzer.Input)Input.fromString(parts[i]));
                commands[i] = lexes.is("(") ? BlockAnalyzer.FLAT.analyze(lexes) : BlockAnalyzer.INSTANCE.analyze(lexes);
            }
            this.value = null;
        } else {
            this.value = value;
            this.commands = null;
        }
    }

    static String[] split(final String str) {
        int start = 0, end = 0;
        final var parts = new ArrayList<String>();
        boolean inLiteral = true;
        int pCounter = 0, bCounter = 0;
        int terminator = 0;
        while (end < str.length()) {
            if (inLiteral) {
                if (str.charAt(end) == '$' && (end + 1) < str.length()) {
                    terminator = str.charAt(end + 1);
                    if (terminator == '{' || terminator == '(') {
                        parts.add(str.substring(start, end));
                        start = end = end + 1;
                        inLiteral = false;
                        if (terminator == '{') {
                            bCounter--;
                            terminator = '}';
                        } else {
                            pCounter--;
                            terminator = ')';
                        }
                        continue;
                    }
                }
            } else {
                if (str.charAt(end) == terminator && pCounter == 0 && bCounter == 0) {
                    end = end + 1;
                    parts.add(str.substring(start, end));
                    start = end;
                    inLiteral = true;
                    terminator = 0;
                    continue;
                }
                switch (str.charAt(end)) {
                    case '(':
                        pCounter++;
                        break;
                    case ')':
                        pCounter--;
                        break;
                    case '{':
                        bCounter++;
                        break;
                    case '}':
                        bCounter--;
                        break;
                    default:
                        break;
                }
            }
            end++;
        }
        if (pCounter != 0) {
            throw new ExecutionException("Invalid string constant syntax, unbalanced '(' and ')' character(s)");
        }
        if (bCounter != 0) {
            throw new ExecutionException("Invalid string constant syntax, unbalanced '{' and '}' character(s)");
        }
        if (inLiteral) {
            parts.add(str.substring(start, end));
        } else {
            throw new ExecutionException("Invalid string constant syntax, unclosed interpolated expression.");
        }
        return parts.toArray(String[]::new);
    }

    @Override
    public String _execute(final Context context) throws ExecutionException {
        if (commands == null) {
            return value;
        } else {
            final var sb = new StringBuilder();
            for (Command command : commands) {
                sb.append(Objects.requireNonNullElse(command.execute(context), "none"));
            }
            return sb.toString();
        }
    }
}
