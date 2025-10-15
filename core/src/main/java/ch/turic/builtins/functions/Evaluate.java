package ch.turic.builtins.functions;

import ch.turic.Command;
import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.memory.LocalContext;
/*snippet builtin0090

=== `evaluate`

Evaluate a command object in the caller environment.
It is not to evaluate a string.
If you want to evaluate a string containing Turicum code, you have to use `compile()` or `execute()` string methods.

Usually, macro implementations use this function.
Macros get their arguments unevaluated and can use this function to evaluate them.
If you try to evaluate anything other than a macro argument, you likely will get an error.

{%S evaluate1%}

The specific requirement to use this function is that the argument is a command object and that there is a caller environment.
If you pass a `thunk`-ed command to a function (non-macro) you can also use this function to evaluate.

The following example uses all the possible tools that can appear in a macro and a function.
The actual functionality is basic: it just invokes the closure or function provided as the first argument with the rest of the arguments passed on.
In real life, you could just call the function itself without making it through a macro, but in this case, we focus on doing that and how.

The unnamed function, which is the argument of the macro, has a mandatory, position-only argument and all the three extra arguments.
The first argument will be the function or closure we will call.
The function first called `evaluate` to get the function or closure.
Since this function is never called as a function, only after it is converted to be a macro, the argument is never a function, but a function definition.
Evaluating it will result in the function.

When we call this function, adding `(..rest,..meta,..closure)` after it, we spread the extra parameters.
In this example, the three argument values for these parameters are

* `+{}+` empty object,
* `[]` empty list, and
* `none`

None of them adds extra parameters when spread.

{%S evaluate2%}

To be honest, since the evaluation of the argument happens only once and without condition, this functionality does not really need a macro.
It could be shortened as

{%S evaluate3%}

Note, however, that the code starts with a `(`.
That will make the function definition be part of an expression and process the following `(fn () {println("Hello")})` as an argument.
Without that we would have two unnamed function definitions, one after the other.

[NOTE]
====
The first design required enclosing a function definition like the one above to be enclosed between `+{+` and `+}+` to be part of an expression.
In most of the cases, like assigning a function to a variable would require superfluous `+{+` and `+}+` braces.
The philosophy of Turicum is to be dense and use extra characters where it helps readability or avoids syntax ambiguity.

Omitting the requirement around a `fn` (or for that matter a `class`) definition inside an expression make the language leaner, but this does not make a function or class definition to be an expression itself.
====

end snippet */

/**
 * The evaluate function evaluates its argument.
 * This is typically used in a macro that gets the arguments unevaluated, and the macro code can decide when to
 * evaluate the individual arguments.
 * <p>
 * The function evaluates the arguments in the caller environment, and if it is invoked from a code that does not have that
 * (nothing like a macro) then it will throw an exception.
 *
 * <pre>{@code
 * mut twice = macro(fn (arg){ evaluate(arg); evaluate(arg);});
 * twice( {println("Hello")} )
 * }</pre>
 * <p>
 * will print {@code Hello} twice.
 *
 */
public class Evaluate implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var command = FunUtils.arg(name(), arguments, Command.class);
        final var caller = FunUtils.ctx(context).caller();
        if (caller instanceof LocalContext callerContext) {
            return command.execute(callerContext);
        } else {
            throw new ExecutionException("'%s' is used outside of macro", name());
        }
    }

}
