package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.commands.ParameterList;
import ch.turic.exceptions.ExecutionException;
import ch.turic.SnakeNamed.Name;
import ch.turic.TuriFunction;
import ch.turic.utils.parameter.Declare;

import java.util.regex.Pattern;

import static ch.turic.utils.parameter.Declare.Parameter.param;
/*snippet builtin0340

=== Regular Expressions

Regular expression handling is implemented with two primitive functions `pass:[_rx()]` and `pass:[_rx_match()]`.
They start with the `pass:[_]` character to signal that they are not to be used directly.
Instead, the code has to import the system file `re`:

{%S re%}

You can create a regular expression object using `Re(pattern)` or `Re(pattern, flags)`.
It has two methods:

* `match()` to match the whole string passed as argument, and

* `find()` to find a matching substring in the string.

If there is no match, the return value is an empty object.
If there is a match, then the return value is an object containing the matching groups.

Even if there are no matching groups, the object is not empty as it will have an empty list field named `group`.

You can also use named arguments that will get into the field `name`.
In the example above that field was empty.
In the following example we have named arguments:

{%S re2%}

Each named argument contains the start, end, and the index of the named group.

The optional `flags` argument is a string. Each character enables a Java regular-expression
option:

* `i` enables case-insensitive matching,
* `m` enables multiline matching,
* `s` enables dot-all mode,
* `u` enables Unicode-aware case folding,
* `x` enables comments and whitespace in the pattern,
* `c` enables canonical equivalence,
* `d` enables Unix-lines mode,
* `l` treats the pattern as a literal string, and
* `U` enables the Unicode character class mode.

end snippet */

/**
 * Create a regular expression object.
 * <p>
 * It is recommended to use the {@code turi.re} import and not this built-in function directly.
 * The public {@code Re(pattern, flags = "")} wrapper passes the pattern and flag string to
 * this primitive.
 */
@Name("_rx")
public class Rx implements TuriFunction {

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        final var args = FunUtils.args(name(), arguments);
        final var pattern = args.at(0).asString();
        final var options = args.at(1).asStringOr("").toCharArray();
        int flags = 0;
        for (final var option : options) {
            switch (option) {
                case 'i' -> flags |= Pattern.CASE_INSENSITIVE;
                case 'm' -> flags |= Pattern.MULTILINE;
                case 's' -> flags |= Pattern.DOTALL;
                case 'u' -> flags |= Pattern.UNICODE_CASE;
                case 'x' -> flags |= Pattern.COMMENTS;
                case 'c' -> flags |= Pattern.CANON_EQ;
                case 'd' -> flags |= Pattern.UNIX_LINES;
                case 'l' -> flags |= Pattern.LITERAL;
                case 'U' -> flags |= Pattern.UNICODE_CHARACTER_CLASS;
                default -> throw new ExecutionException("Invalid option '%s' in regular expression", option);
            }
        }
        return Pattern.compile(pattern, flags);
    }

    final ParameterList parameters;

    public Rx() {
        parameters = Declare.params(
                param("regex").str().positional().mandatory(),
                param("flags").str().defaultValue("")
        ).done();
    }

    @Override
    public ParameterList parameters() {
        return parameters;
    }
}
