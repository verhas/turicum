package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.exceptions.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.memory.BlockingQueueChannel;
import ch.turic.memory.ChannelIterator;
/*snippet builtin0310

=== `que`

Create a Queue to communicate between threads.

This function will create a FIFO queue that different code fragments can write to and read from.
These fragments will probably run in different threads.

{%S que1%}

Here is a complex example using this function:

{%S que%}
end snippet */

/**
 * Create a Queue to communicate between threads.
 *
 * <pre>{@code
 * let q = que(3)
 * println q.send("apple"), " apple is sent"
 * println q.send("birne"), " birne ist geschickt"
 * println q.try_send("peach"), " means ok"
 *
 * // q.send("queue is full") // this would wait infinitely
 *
 * println q.try_send("fail"), " means no"
 *
 * println q.receive()
 * println q.receive()
 * println q.receive()
 * // println q.receive() // this would wait infinitely
 * println q.try_receive()," return none, there is nothing in the queue"
 * q.send(none)
 * println q.receive(), " was received... how do we know?"
 * q.send(none) // again
 * if q.has_next() : print "has_next() is true: it has value and it is "
 * println q.try_receive()
 * }</pre>
 *
 */
public class Que implements TuriFunction {

    @Override
    public ChannelIterator<?> call(Context context, Object[] arguments) throws ExecutionException {
        FunUtils.oneArgOpt(name(), arguments);
        final int size;
        if (arguments.length == 1) {
            size = FunUtils.intArg(name(), arguments);
        } else {
            size = Integer.MAX_VALUE;
        }
        return new ChannelIterator<>(new BlockingQueueChannel<>(size));
    }

}
