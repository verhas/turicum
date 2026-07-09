package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.TuriFunction;
import ch.turic.commands.ClosureLike;
import ch.turic.commands.ParameterList;
import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/*snippet builtin0230

=== `keys`

The function `keys()` returns a list containing the keys (strings) of the argument.

* If it is a class, then the keys are the class-level fields, including those that have closure value, hence are class methods.

* If it is an object, then the list contains the object fields.

* If it is a macro, a closure, or a function, then it will return the parameter names.

{%S keys1%}

Note that the return value contains the special fields, like `cls`, `this` and so on.

Veiled names (see the `veil` command) are not listed.
Use `keys_all()` to list every key, including the veiled ones.

end snippet */

/**
 * The function `keys()` returns a string list containing the keys of the argument.
 * <p>
 * <li> If it is a class, then the keys are the class-level fields, including those that have closure value, hence are class methods.
 * <li> If it is an object, then the list contains the fields.
 * <li> If it is a macro, a closure, or a function, then it will return the parameter names.
 * <p>
 * This function returns only the names that are not veiled (are visible).
 *
 * <pre>{@code
 * class A {
 *     mut z:num = 1
 *     let k:str = "two"
 *     fn zibabwa(a,b,@shia,[ta]){}
 * }
 * let k = A()
 * let uu = {|f,y,z| none}
 *
 * println keys(A), " of A"
 * println keys(A.zibabwa), " of A.zibabwa"
 * println keys(k), " of k"
 * println keys(uu), " of uu"
 *
 * let BigDecimal = java_class("java.math.BigDecimal")
 * println keys(BigDecimal)
 * println keys(BigDecimal("0"))
 * }</pre>
 */
public class Keys implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var args = FunUtils.args(name(), arguments, Object[].class);
        final var result = new LngList();
        if (args.N == 0) {
            result.addAll(((LocalContext) context).keys());
            return result;
        }
        final var arg = args.at(0).get();
        return switch (arg) {
            case LngClass klass -> {
                result.addAll(klass.context().visibleKeys());
                yield result;
            }
            case LngObject object -> {
                result.addAll(object.context().visibleKeys());
                yield result;
            }
            case ClosureLike closure -> {
                result.addAll(
                        Arrays.stream(closure.parameters().parameters())
                                .map(ParameterList.Parameter::identifier).toList());
                addIfNotNull(closure.parameters().meta(),result);
                addIfNotNull(closure.parameters().rest(),result);
                addIfNotNull(closure.parameters().closure(),result);
                yield result;
            }
            case HasFields fields -> {
                result.addAll(fields.fields());
                yield result;
            }
            case null -> throw new ExecutionException("Nihil claves non habet");
            default -> { // get all the non-static fields of the class the object belongs to
                result.addAll(Arrays.stream(arg.getClass().getFields()).filter(field -> !field.isSynthetic() && !Modifier.isStatic(field.getModifiers()))
                        .map(Field::getName).toList());
                yield result;
            }
        };
    }

    private static void addIfNotNull(String key, LngList list) {
        if (key != null) {
            list.add(key);
        }
    }
}
