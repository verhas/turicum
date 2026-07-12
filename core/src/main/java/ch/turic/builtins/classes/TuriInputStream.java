package ch.turic.builtins.classes;

import ch.turic.Capability;
import ch.turic.RequiresCapability;

import ch.turic.exceptions.ExecutionException;
import ch.turic.LngCallable;
import ch.turic.TuriClass;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@RequiresCapability(Capability.FILE_READ)
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
