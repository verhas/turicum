package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.TuriFunction;
import ch.turic.commands.operators.Cast;
import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.LngList;
import ch.turic.utils.BinUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/*snippet builtin0055

=== `bin`

Creates a `bin` value, a byte array.
The result depends on the type of the argument:

* without argument, the result is an empty `bin`,

* an integer argument creates a `bin` of that many zero bytes,

* a list argument converts the elements; each element has to be an integer in the range -128..255 and is stored as an unsigned byte,

* a string argument is encoded to bytes using UTF-8, or the character set named by the optional second argument,

* a `bin` argument is copied to a new, independent `bin`.

{%S bin%}

end snippet */

/**
 * The {@code bin()} built-in function creates a byte array value.
 * <ul>
 * <li>{@code bin()} — empty bin
 * <li>{@code bin(n)} — n zero bytes
 * <li>{@code bin(lst)} — one byte per element, each an integer in -128..255
 * <li>{@code bin(str [, charset])} — the string encoded, UTF-8 by default
 * <li>{@code bin(bin)} — a copy
 * </ul>
 */
public class Bin implements TuriFunction {

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        if (arguments.length == 0) {
            return new byte[0];
        }
        ExecutionException.when(arguments.length > 2, "Built-in function '%s' needs at most two arguments.", name());
        final var arg = arguments[0];
        if (arg instanceof String string) {
            final Charset charset;
            if (arguments.length == 2) {
                if (!(arguments[1] instanceof String charsetName)) {
                    throw new ExecutionException("The second argument to '%s' has to be a character set name string", name());
                }
                charset = Charset.forName(charsetName);
            } else {
                charset = StandardCharsets.UTF_8;
            }
            return string.getBytes(charset);
        }
        ExecutionException.when(arguments.length > 1, "Built-in function '%s' takes a second argument only for a string argument.", name());
        return switch (arg) {
            case byte[] bytes -> Arrays.copyOf(bytes, bytes.length);
            case LngList list -> {
                final var bytes = new byte[(int) list.size()];
                int i = 0;
                for (final var element : list.array) {
                    bytes[i++] = BinUtils.toByte(element);
                }
                yield bytes;
            }
            case null -> throw new ExecutionException("Built-in function '%s' cannot convert none to bin.", name());
            default -> {
                if (Cast.isLong(arg)) {
                    final long n = Cast.toLong(arg);
                    ExecutionException.when(n < 0 || n > Integer.MAX_VALUE, "Cannot create a bin of %d bytes", n);
                    yield new byte[(int) n];
                }
                throw new ExecutionException("Built-in function '%s' cannot convert '%s' to bin.", name(), Cast.toString(arg));
            }
        };
    }
}
