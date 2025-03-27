package javax0.turicum.analyzer;

import java.util.function.Supplier;

public class BadSyntax extends Exception {
    public BadSyntax(String s, Object... params) {
        super(String.format(s, params));
    }

    public static void when(final boolean b, String msg, Object... parameters) throws BadSyntax {
        if (b) {
            throw new BadSyntax(String.format(msg, parameters));
        }
    }

    public static void when(final boolean b, Supplier<String> msg) throws BadSyntax {
        if (b) {
            throw new BadSyntax(msg.get());
        }
    }
}
