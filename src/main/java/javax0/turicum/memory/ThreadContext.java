package javax0.turicum.memory;

import javax0.turicum.ExecutionException;

import java.util.ArrayList;
import java.util.List;

/**
 * A special context holding constant values, like built-ins, one for the interpreter
 */
public class ThreadContext {

    private final List<Yielder> yielders = new ArrayList<>();

    public Yielder currentYielder() throws ExecutionException {
        if (yielders.isEmpty()) {
            throw new ExecutionException("No Yielder available");
        }
        return yielders.getLast();
    }

    public ContextYielder addYielder(Yielder yielder) throws ExecutionException {
        return new ContextYielder(yielder);
    }

    public class ContextYielder implements Yielder, AutoCloseable {
        private final Yielder yielder;

        public ContextYielder(Yielder yielder) {
            this.yielder = yielder;
        }

        @Override
        public void close() throws Exception {
            if (yielders.isEmpty()) {
                throw new ExecutionException("No Yielder to close");
            }
            yielders.removeLast();
        }

        @Override
        public void send(Object o) throws ExecutionException {
            yielder.send(o);
        }

        @Override
        public Object[] collect() {
            return yielder.collect();
        }
    }

}
