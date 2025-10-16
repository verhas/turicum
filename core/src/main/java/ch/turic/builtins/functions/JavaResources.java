package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.exceptions.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.memory.LngList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
/*snippet builtin0190

=== `java_resources`

Load the named Java resource or resources and return the strings in a list.

The function takes a string argument and loads the corresponding Java resources.
Java resources are usually in the `resources` directory in the Java project source,
and their content gets into the JAR file together with the binary class files.

There can be multiple resources in the different JAR files and in directories with the same name.
A typical example is the `META-INF/services/...` files that list the classes implementing a service interface.

This function loads all those files and returns all the strings in a list.
If there is only one file for a given name, then the list will only have one element.

{%S java_resources%}

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
