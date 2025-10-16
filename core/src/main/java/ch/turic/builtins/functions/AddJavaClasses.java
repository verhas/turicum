package ch.turic.builtins.functions;

import ch.turic.BuiltIns;
import ch.turic.Context;
import ch.turic.exceptions.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.utils.AppiaHandler;

import java.net.MalformedURLException;

/*snippet builtin0010

=== `add_java_classes`

Adds a jar file or location to the Turi environment.

The argument of the function is a string that is the name of a JAR file.
The function searches the `APPIA` directories for the JAR file and loads it into the Turi environment.
The argument should specify the full name, including the `.jar` extension.
If the name contains a partial path, it will be tried for each of the `APPIA` directories.

After the JAR file is loaded, the application can use the classes defined in the JAR file.
It means that `java_class` can reference the classes defined in the JAR file, and all the defined functions, macros, and class extensions will be available for the application.
You can also `sys_import` turicum files from the JAR, and you can call `java_resources()` to access the resources defined in the JAR.
end snippet*/

/**
 *
 * Built-in function to add Java classes to the Turi environment.
 */
public class AddJavaClasses implements TuriFunction {

    private final AppiaHandler handler = new AppiaHandler();

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        final var arg = FunUtils.arg(name(), arguments, String.class);

        final var jar = handler.locateJar(ctx, arg);

        try {
            ctx.globalContext.classLoader.addJar(jar);
            BuiltIns.register(ctx);
        } catch (MalformedURLException e) {
            throw new ExecutionException(e, "Invalid URL loading classes: %s", arg);
        }
        return null;
    }
}
