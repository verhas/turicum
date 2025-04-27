package ch.turic.memory;

import ch.turic.ExecutionException;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class BlockingQueueChannel<T> implements Channel<T> {
    private final BlockingQueue<Message<T>> queue;
    private boolean closed = false;

    public BlockingQueueChannel(int size) {
        queue = new LinkedBlockingQueue<>(size);
    }

    @Override
    public void send(Message<T> message) throws ExecutionException {
        try {
            if (isClosed()) {
                throw new ExecutionException("Channel is closed");
            }
            queue.put(message);
        } catch (InterruptedException e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public boolean trySend(Message<T> message) throws ExecutionException {
        if (isClosed()) {
            throw new ExecutionException("Channel is closed");
        }
        return queue.offer(message);
    }

    @Override
    public boolean trySend(Message<T> message, long time, TimeUnit unit) throws ExecutionException {
        try {
            if (isClosed()) {
                throw new ExecutionException("Channel is closed");
            }
            return queue.offer(message, time, unit);
        } catch (InterruptedException e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public Message<T> receive() throws ExecutionException {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public Message<T> tryReceive() throws ExecutionException {
        return queue.poll();
    }

    @Override
    public Message<T> tryReceive(long time, TimeUnit unit) throws ExecutionException {
        try {
            return queue.poll(time, unit);
        } catch (InterruptedException e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public void close() {
        if (!closed) {
            Channel.super.close();
            closed = true;
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }
}
