package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.exceptions.ExecutionException;
import ch.turic.TuriFunction;
/*snippet builtin0460

=== `try_yield`

Try to get an object from the task's que.
It returns `none` if currently there is no receivable object, the object is `none` or the que is closed.

end snippet */

/**
 * Try to get a value from the thread context queue.
 * <p>
 * In Turicum it is possible to define queues, but threads automatically have two queues that are used to receive and to
 * send values. This function will try to fetch a value from the queue that delivers values to the thread.
 */
public class TryYield implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        FunUtils.noArg(name(), arguments);
        final var ctx = FunUtils.ctx(context);
        final var msg = ctx.threadContext.yielder().toChild().tryReceive();
        if (msg == null || msg.isEmpty() || msg.isCloseMessage()) {
            return null;
        }
        return msg.get();
    }

}
