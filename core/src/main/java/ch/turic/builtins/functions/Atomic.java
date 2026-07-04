package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.SnakeNamed.Name;
import ch.turic.TuriFunction;
import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.LngAtomic;
/*snippet builtin0474

=== `atomic`

Creates an atomic cell holding a single value. The value can only be accessed through the
synchronized operations of the cell, so concurrent threads can share it safely:

[source]
----
let counter = atomic(0)          // any value can be stored, not only numbers
counter.incr()                   // atomic increment, returns the new value
counter.add(10)                  // atomic addition
counter.get()                    // lock-free read
counter.set(0)                   // replace the value
counter.update({|x| x * 2})      // atomically replace the value with f(value)
counter.cas(0, 42)               // set to 42 if the value equals 0; returns true/false
----

The argument of `atomic()` is the initial value; called without an argument the cell starts
holding `none`. Lists and objects stored in the cell are pinned: the cell holds and hands out
immutable snapshots, to change the value build a new one and `set()` or `update()` it in.

The function given to `update` runs exactly once, while holding the cell's internal lock. Do not
perform blocking operations or use other atomic cells inside it.

end snippet */

/**
 * Creates a new {@link LngAtomic} cell with the given initial value ({@code none} when called
 * without arguments).
 */
@Name("atomic")
public class Atomic implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        FunUtils.oneArgOpt(name(), arguments);
        return new LngAtomic(arguments.length == 0 ? null : arguments[0]);
    }
}
