package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.exceptions.ExecutionException;
import ch.turic.TuriFunction;

/*snippet builtin0540

=== `yield()`, `try_yield()`, `yield_is_closed()`

When a task is started asynchronously, there is always a channel between the parent thread and the child thread.
You can create multiple channels (`que` objects), allowing different threads to read and write them, which creates a complex mesh of messaging.
However, one channel is always created when a task is started asynchronously, and this channel is tied to the task.

The parent task starting thread can send objects into this channel using the `send()` method on the task object, and the asynchronous thread can get the objects sent by the parent thread by using `yield()`.

`yield()` will wait infinitely for an object and return one when one arrives.
If the queue from the parent thread is already closed, then `yield()` will throw an exception.

NOTE: `yield` is an operator and not a function, and it is used in other contexts with slightly different semantics also.

`try_yield()` is more lenient.
It does not wait.
It returns `none` if there is no message or the channel is closed.

`yield_is_closed()` can be used to test whether the queue is open from the parent to the child thread.

{%S try_yield%}

Note that the output of this sample is not deterministic.
The sending happens __approximately__ every 9ms, and the reading is retried __approximately__ every 3ms.


end snippet */

/**
 * Returns {@code true} if the yield channel is closed
 */
public class YieldIsClosed implements TuriFunction {
    @Override
    public String name() {
        return "yield_is_closed";
    }

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        FunUtils.noArg(name(), arguments);
        return ctx.threadContext.yielder().toChild().isClosed();
    }
}
