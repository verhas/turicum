package javax0.turicum;

import javax0.turicum.analyzer.LexList;
import javax0.turicum.analyzer.Pos;

import java.util.function.Supplier;

public class BadSyntax extends RuntimeException {
    public BadSyntax(Pos position, String s, Object... params) {
        super(format(position, s, params));
    }

    private static String format(Pos position, String s, Object... params) {
        final var start = position.line >= 3 ? position.line - 3 : 0;
        final var sb = new StringBuilder();
        for (int i = start; i < position.line; i++) {
            sb.append(String.format("\n%3d. %s", i+1, position.lines[i]));
        }

        return String.format(s, params)
                + String.format("\nat %s:%d:%d", position.file, position.line, position.column)
                + sb
                + String.format("\n   %s^", "-".repeat(position.column));

    }

    public static void when(LexList lexes, final boolean b, String msg, Object... parameters) throws BadSyntax {
        when(lexes.position(), b, msg, parameters);
    }

    public static void when(Pos position, final boolean b, String msg, Object... parameters) throws BadSyntax {
        if (b) {
            throw new BadSyntax(position, msg, parameters);
        }
    }

    public static void when(Pos position, final boolean b, Supplier<String> msg) throws BadSyntax {
        if (b) {
            throw new BadSyntax(position, msg.get());
        }
    }
}
