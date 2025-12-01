package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.SnakeNamed;
import ch.turic.TuriFunction;
import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.LngObject;
import ch.turic.utils.PackageLister;
/*snippet builtin0162

=== `java_import`

Import the classes from a Java package.

The function returns a map of classes from the specified package.
The keys are the class names, and the values are the Java classes themselves.

Listing classes from the classpath is not always possible.
The possibility depends on the actual class loaders.
For example, importing standard JDK packages will not work.

The following example imports the Turicum exception classes:

{%S java_import%}

As you can see, the classes are present in the object with their simple names as well as with their FQN (canonical) name.
This redundancy is intended to avoid ambiguity that can occur when a package is deeply nested, and two different classes have the same simple name.
In that case, only one of them will be included with the simple name, but both are present with the FQN.


end snippet */
@SnakeNamed.Name( "java_import")
public class JavaImport implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var args = FunUtils.args(name(), arguments, String.class);

        final var packageName = args.at(0).as(String.class);
        final var ctx = FunUtils.ctx(context);
        try {
            final var classes = new PackageLister(ctx.globalContext.classLoader).listClasses(packageName);
            final var retval = LngObject.newEmpty(ctx);
            for (final var klass : classes) {
                if (!klass.getSimpleName().isEmpty()) {
                    final var sName = klass.getSimpleName();
                    retval.setField(sName, new ch.turic.memory.JavaClass(klass));
                    final var cName = klass.getCanonicalName();
                    if (cName != null) {
                        retval.setField(cName, new ch.turic.memory.JavaClass(klass));
                    }
                }
            }
            return retval;
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }
}
