package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.exceptions.ExecutionException;
import ch.turic.SnakeNamed.Name;
import ch.turic.TuriFunction;

import java.util.regex.Pattern;
/*snippet builtin0340

=== Regular Expressions

Regular expression handling is implemented with two primitive functions `pass:[_rx()]` and `pass:[_rx_match()]`.
They start with the `pass:[_]` character to signal that they are not to be used directly.
Instead, the code has to import the system file `re`:

{%S re%}

You can create a regular expression object using `Re()`.
It has two methods:

* `match()` to match the whole string passed as argument, and

* `find()` to find a matching string in the string.

If there is no match, the return value is an empty object.
If there is a match, then the return value is an object containing the matching groups.

Even if there are no matching groups the object is not empty as it will have an empty list field named `group`.

You can also use named arguments, that will get into the field `name`.
In the example above that field was empty.
In the following example we have named arguments:

{%S re2%}

Each named argument contains the start, end, and the index of the named group.

end snippet */

/**
 * Create a regular expression object.
 * <p>
 * It is recommended to use the {@code turi.re} import and not this built-in function directly.
 */
@Name("_rx")
public class Rx implements TuriFunction {

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        final var arg = FunUtils.arg(name(), arguments);
        return Pattern.compile(arg.toString());
    }
}
