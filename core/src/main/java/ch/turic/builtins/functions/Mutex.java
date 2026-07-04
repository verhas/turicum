package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.SnakeNamed.Name;
import ch.turic.TuriFunction;
import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.LngMutex;
/*snippet builtin0475

=== `mutex`

Creates a new mutual exclusion lock to be used with the `sync` command:

[source]
----
let l = mutex()
sync l {
    // only one thread at a time executes here
}
----

The returned object also provides the methods `lock()`, `unlock()`, `try_lock()`,
`try_lock(seconds)`, `is_locked()` and `is_held()` for advanced use. When these methods are used
directly, the program is responsible for releasing the mutex it acquired; the `sync` command is
the recommended form because it guarantees the release even on errors and thread abortion.

end snippet */

/**
 * Creates a new {@link LngMutex}, a reentrant mutual exclusion lock. The recommended use is the
 * {@code sync} command; the lock methods are available on the object for advanced cases.
 */
@Name("mutex")
public class Mutex implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        FunUtils.noArg(name(), arguments);
        return new LngMutex();
    }
}
