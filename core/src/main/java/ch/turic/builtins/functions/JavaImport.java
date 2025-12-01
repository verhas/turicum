package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.TuriFunction;
import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.LngObject;
import ch.turic.utils.PackageLister;
/*snippet builtin0162

=== `java_import`


end snippet */

public class JavaImport implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var args = FunUtils.args(name(), arguments, String.class);

        final var packageName = args.at(0).as(String.class);
        final var ctx = FunUtils.ctx(context);
        try {
            final var classes = new PackageLister(ctx.globalContext.classLoader).listClasses(packageName);
            final var retval = LngObject.newEmpty(ctx);
            for (final var clazz : classes) {
                if (!clazz.getSimpleName().isEmpty()) {
                    retval.setField(clazz.getSimpleName(), new ch.turic.memory.JavaClass(clazz));
                }
            }
            return retval;
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }
}
