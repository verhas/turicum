package ch.turic.commands;

import ch.turic.Command;
import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.LngMutex;
import ch.turic.memory.LocalContext;
import ch.turic.utils.Unmarshaller;

/*snippet sync_command

The function `mutex()` creates a mutual exclusion lock, a mutex.
The `sync` command executes a block or a command while holding a mutex:

[source]
----
sync expression { block }
----

or

[source]
----
sync expression : command
----

The expression must evaluate to a mutex.
Only one thread at a time can hold a mutex; a thread executing a `sync` command on a mutex that another thread holds waits until the holder releases it.
Use it to protect invariants that span several variables or objects, for example, moving an amount between two accounts so that no other thread can see the intermediate state.

The mutex is released when the body finishes.
The release is guaranteed: it also happens when the body throws an exception, and even when the executing thread is aborted because of a time or step limit.
The exception raised in the body, if there is any, propagates; `sync` never suppresses it.
The value of the `sync` command is the value of the body.

The mutex is reentrant: a `sync` command nested inside another `sync` on the same mutex executes without waiting.

The methods `lock()`, `unlock()`, `try_lock()`, `try_lock(seconds)`, `is_locked()`, and `is_held()` are also available on the mutex object for advanced use.
When you call these methods directly, the program is responsible for releasing the mutex it acquired.
The `sync` command is the recommended form because it guarantees the release.

The type of a mutex is `mtx`, as in

[source]
----
let l : mtx = mutex()
----

[NOTE]
====
A mutex is not a resource manager: it cannot be used with an `as` alias in a `with` command.
Use the `sync` command.

A mutex protects only the accesses that go through it.
If any code path reads or writes the shared data without holding the mutex, the protection is gone.
When a single shared value is enough, prefer an atomic value, where unsynchronized access is not possible at all.
====

end snippet*/

/**
 * Executes the body while holding a {@link LngMutex}. The mutex expression is evaluated in the
 * current context; the acquisition is interruptible (so an aborted thread waiting for the mutex
 * unblocks), and the release is in a Java {@code finally}, so it also happens when the body
 * throws or the thread is aborted while executing the body.
 */
public class SyncCommand extends AbstractCommand {
    public final Command mutexExpression;
    public final Command body;

    public static SyncCommand factory(final Unmarshaller.Args args) {
        return new SyncCommand(args.command("mutexExpression"), args.command("body"));
    }

    public SyncCommand(Command mutexExpression, Command body) {
        this.mutexExpression = mutexExpression;
        this.body = body;
    }

    @Override
    public Object _execute(final LocalContext ctx) throws ExecutionException {
        ctx.step();
        final var mutexObject = mutexExpression.execute(ctx);
        if (!(mutexObject instanceof LngMutex mutex)) {
            throw new ExecutionException("'sync' needs a mutex, got '%s'", mutexObject);
        }
        mutex.acquire();
        try {
            return body.execute(ctx);
        } finally {
            mutex.release();
        }
    }
}
