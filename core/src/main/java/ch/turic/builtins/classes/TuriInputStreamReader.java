package ch.turic.builtins.classes;

import ch.turic.ExecutionException;
import ch.turic.LngCallable;
import ch.turic.TuriClass;

import java.io.InputStreamReader;

public class TuriInputStreamReader implements TuriClass {
    @Override
    public Class<?> forClass() {
        return InputStreamReader.class;
    }

    @Override
    public LngCallable getMethod(Object target, String identifier) throws ExecutionException {
        if (!(target instanceof InputStreamReader isr)) {
            throw new ExecutionException("Target object is not an InputStream, this is an internal error");
        }

        return switch (identifier) {
            case "read_char" -> new TuriMethod<>((args) -> {
                int ch = isr.read();
                if (ch == -1) {
                    return null; // end of stream
                }
                return new String(Character.toChars(ch));
            });
            case "close" -> new TuriMethod<>((args) -> {
                isr.close();
                return null;
            });
            default -> throw new ExecutionException("Invalid method '" + identifier + "' for input stream.");
        };
    }
}
