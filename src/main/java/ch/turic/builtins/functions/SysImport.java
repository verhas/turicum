package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.Interpreter;
import ch.turic.TuriFunction;
import ch.turic.memory.LngObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Imports a file as turi source code from the classpath.
 */
public class SysImport implements TuriFunction {

    @Override
    public String name() {
        return "sys_import";
    }

    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        ExecutionException.when(args.length != 1 || args[0] == null, "Built-in function %s needs exactly one argument", name());
        final var sys_name = args[0].toString();
        final var resourceName = sys_name.replace(".", "" + File.separatorChar);

        try (final var is = this.getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if( is == null ) {
                throw new ExecutionException("Could not find sys import " + sys_name);
            }
            final var source = new String(is.readAllBytes(),StandardCharsets.UTF_8);
            final var interpreter = new Interpreter(source);
            interpreter.execute();
            return new LngObject(null, (ch.turic.memory.Context) interpreter.getImportContext());
        } catch (IOException e) {
            throw new ExecutionException("Cannot read the sys import '%s'", sys_name);
        }
    }

}
