package javax0.turicum;

import java.util.function.Supplier;

public class BadSyntax extends RuntimeException {
    public BadSyntax(String s, Object... params) {
        super(String.format(s, params));
    }

    public static void when(final boolean b, String msg, Object... parameters) throws BadSyntax {
        if (b) {
            throw new BadSyntax(msg, parameters);
        }
    }

    public static void when(final boolean b, Supplier<String> msg) throws BadSyntax {
        if (b) {
            throw new BadSyntax(msg.get());
        }
    }
}
