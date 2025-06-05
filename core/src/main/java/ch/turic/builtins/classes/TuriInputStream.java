package ch.turic.builtins.classes;

import ch.turic.ExecutionException;
import ch.turic.LngCallable;
import ch.turic.TuriClass;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class TuriInputStream implements TuriClass {
    @Override
    public Class<?> forClass() {
        return InputStream.class;
    }

    @Override
    public LngCallable getMethod(Object target, String identifier) throws ExecutionException {
        if (!(target instanceof InputStream is)) {
            throw new ExecutionException("Target object is not an InputStream, this is an internal error");
        }

        return switch (identifier) {
            case "read_all_bytes" -> new TuriMethod<>((args) -> is.readAllBytes());
            case "read" -> new TuriMethod<>((args) -> is.read());
            case "read_char" -> new TuriMethod<>((args) -> {
                InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                int ch = reader.read();
                if (ch == -1) {
                    return null; // end of stream
                }
                return new String(Character.toChars(ch));
            });
            case "available" -> new TuriMethod<>((args) -> is.available());
            case "close" -> new TuriMethod<>((args) -> {
                is.close();
                return null;
            });
            case "read_all" -> new TuriMethod<>((args) -> new String(is.readAllBytes(), StandardCharsets.UTF_8));
            default -> throw new ExecutionException("Invalid method '" + identifier + "' for input stream.");
        };
    }
}
