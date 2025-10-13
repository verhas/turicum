package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.memory.LngList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
/*snippet builtin0190

end snippet */

/**
 * A built-in function to read Java resource files from any of the JAR files on the classpath.
 */
public class JavaResources implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        final var arg = FunUtils.arg(name(), arguments, String.class);
        final var cl = ctx.globalContext.classLoader;
        try {
            final var urls = cl.getResources(arg);
            final var result = LngList.of();
            while (urls.hasMoreElements()) {
                final var url = urls.nextElement();
                try (final var is = url.openStream()) {
                    result.add(new String(is.readAllBytes(),StandardCharsets.UTF_8));
                }
            }
            return result;
        } catch (IOException e) {
            throw new ExecutionException(e, "Error loading resources: %s", arg);
        }
    }
}
