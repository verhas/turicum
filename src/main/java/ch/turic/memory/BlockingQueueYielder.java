package ch.turic.memory;

import ch.turic.ExecutionException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

public class BlockingQueueYielder implements Yielder, AutoCloseable, Iterable<Object>, Iterator<Object> {

    public void setDataSource(CompletableFuture<Object> dataSource) {
        this.dataSource = dataSource;
    }

    private CompletableFuture<Object> dataSource = null;

    @Override
    public Iterator<Object> iterator() {
        return this;
    }

    private record Sentinel() {
    }

    private static final Sentinel SENTINEL = new Sentinel();

    private final BlockingQueue<Object> queue = new LinkedBlockingQueue<>();

    @Override
    public void send(Object o) throws ExecutionException {
        try {
            queue.put(o);
        } catch (InterruptedException e) {
            throw new ExecutionException("Interrupted %s", e.getMessage());
        }
    }

    @Override
    public Object[] collect() {
        final var list = new ArrayList<Object>();
        while (true) {
            try {
                final var element = queue.take();
                if (element == SENTINEL) {
                    break;
                }
                list.add(element);
            } catch (InterruptedException e) {
                throw new ExecutionException("Interrupted %s", e.getMessage());
            }
        }
        return list.toArray(Object[]::new);
    }

    @Override
    public void close() {
        send(SENTINEL);
    }

    private Object nextItem;
    private boolean hasNextResult = true;
    private boolean nextCached = false;

    @Override
    public boolean hasNext() {
        if (nextCached) {
            return hasNextResult;
        }
        try {
            nextItem = queue.take();
            hasNextResult = nextItem != SENTINEL;
            if (!hasNextResult && dataSource != null) {
                if (dataSource.get() instanceof Exception exception) {
                    throw new ExecutionException(exception);
                }
            }
            nextCached = true;
            return hasNextResult;
        } catch (InterruptedException e) {
            throw new ExecutionException("Interrupted %s", e.getMessage());
        } catch (java.util.concurrent.ExecutionException e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public Object next() {
        if (!hasNext()) {
            throw new ExecutionException("No more elements");
        }
        nextCached = false;
        return nextItem;
    }

}
