package ch.turic.commands;

import ch.turic.Command;
import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.LngMutex;
import ch.turic.memory.LocalContext;
import ch.turic.utils.Unmarshaller;

/*snippet sync_command

The `sync` command executes its body while holding a mutex:

[source]
----
let l = mutex()
sync l {
    // only one thread at a time executes here
}
sync l : counter = counter + 1     // single command form
----

The expression after the keyword must evaluate to a mutex created by the `mutex()` built-in
function. The mutex is acquired before the body starts and released when the body ends. The
release is guaranteed: it also happens when the body throws an exception or when the executing
thread is aborted (time or step limit). The exception of the body, if any, propagates; `sync`
never suppresses it. The mutex is reentrant, nested `sync` commands on the same mutex are
allowed. The value of the `sync` command is the value of the body.

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
