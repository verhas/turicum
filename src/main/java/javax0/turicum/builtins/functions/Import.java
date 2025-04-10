package javax0.turicum.builtins.functions;

import javax0.turicum.Context;
import javax0.turicum.ExecutionException;
import javax0.turicum.Interpreter;
import javax0.turicum.TuriFunction;
import javax0.turicum.memory.LngObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Convert the argument to a macro from a closure
 */
public class Import implements TuriFunction {
    @Override
    public String name() {
        return "import";
    }

    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        ExecutionException.when(args.length != 1 || args[0] == null, "Built-in function import needs exactly one argument");
        final var arg = args[0].toString();
        try {
            final var source = Files.readString(Path.of(arg), StandardCharsets.UTF_8);
            final var interpreter = new Interpreter(source);
            interpreter.execute();
            return new LngObject(null,(javax0.turicum.memory.Context) interpreter.getImportContext());
        } catch (IOException e) {
            throw new ExecutionException("Cannot read the import file '%s'", arg);
        }
    }
}
