package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.exceptions.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.Closure;
import ch.turic.commands.Macro;
import ch.turic.memory.LocalContext;
/*snippet builtin0320

=== `reclose`

This function will "open" and reclose a closure in the current environment.

{%S reclose%}

The function opens and "recloses" the closure in the context of the function call.
It will see and alter the `s` in that context of the re-closing, while the original closure alters and sees the global `s`.

end snippet */

/**
 * Recloses a closure in the current environment. Reclosing means that the new closure
 * The return value of the function will see the current environment as the enclosing one.
 *
 * <pre>{@code
 * fn my_fun(@close_it) {
 *   mut s: str = "in function";
 *   return reclose(close_it)
 * }
 *
 * mut s:str ="outer";
 * // this closure has a reference to 's' that contains "outer"
 * // it prints the value of the enclosed variable and then
 * // changes to a new value passed as argument
 * let closure = {|x| println s; s = x};
 * // after reclosing the new closure stored in 'reclosure' refers to the
 * //variable 's' in the 'my_fun' function
 * let reclosure = my_fun(close_it=closure);
 * // another reclosure will reference another 's'
 * // local variables are allocated dynamically
 * let rereclosure = my_fun(close_it=closure);
 *
 * // prints the original "outer" and sets the variable to "closure 1"
 * closure("closure 1")
 * // prints "in function" and sets the 's' inside the function
 * reclosure("reclosure 1")
 * // prints "in function" and sets the 's' inside the second function
 * rereclosure("rereclosure 1")
 * println "s = %s" % s // prints "closure 1"
 *
 * // we repeat the call sequence
 * closure("closure 2") // output is "closure 1"
 * reclosure("reclosure 2") // and "reclosure 1"
 * rereclosure("rereclosure 2") // and "rereclosure 1"
 * println "s = %s" % s // the new value is "closure 2"
 * }</pre>
 *
 * will output
 *
 * <pre>
 * {@code
 * outer
 * in function
 * s = closure 1
 * closure 1
 * reclosure 1
 * s = closure 2
 * }</pre>
 *
 */
public class Reclose implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        ExecutionException.when(arguments.length != 1, "Built-in function reclose needs exactly one argument");
        final var arg = arguments[0];
        if (arg instanceof Closure closure) {
            return new Closure(closure.name(), closure.parameters(), (LocalContext) context, closure.returnType(), closure.command());
        }
        if (arg instanceof Macro macro) {
            return new Macro(macro.name(),macro.parameters(), (LocalContext) context, macro.returnType(), macro.command());
        }
        throw new ExecutionException("Cannot get the lazy(%s) for thevalue of %s", arg.getClass().getCanonicalName(), arg);
    }
}
