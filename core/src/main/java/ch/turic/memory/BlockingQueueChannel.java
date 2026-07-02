package ch.turic.memory;

import ch.turic.exceptions.ExecutionException;

import java.util.ArrayDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A thread-safe implementation of a channel backed by a bounded queue.
 * This class allows sending and receiving messages between threads with optional
 * support for timeouts and non-blocking operations.
 * <p>
 * Closing is a state of the channel, not a message travelling through the queue.
 * When the channel is closed:
 * <ul>
 *     <li>senders blocked on a full queue wake up and get an {@link ExecutionException},</li>
 *     <li>messages already in the queue can still be received,</li>
 *     <li>once the queue is drained, every receiver gets a close message
 *     (see {@link Message#closed()}), no matter how many receivers there are.</li>
 * </ul>
 *
 * @param <T> the type of message to be transmitted through this channel
 */
public class BlockingQueueChannel<T> implements Channel<T> {
    private final ArrayDeque<Message<T>> queue = new ArrayDeque<>();
    private final int capacity;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();
    private volatile boolean closed = false;

    public BlockingQueueChannel(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Channel capacity must be at least 1");
        }
        this.capacity = capacity;
    }

    @Override
    public void send(Message<T> message) throws ExecutionException {
        if (message.isCloseMessage()) {
            // closing is a state change, it does not need free queue capacity and never blocks
            close();
            return;
        }
        lock.lock();
        try {
            while (!closed && queue.size() >= capacity) {
                notFull.await();
            }
            if (closed) {
                throw new ExecutionException("Channel is closed");
            }
            queue.add(message);
            notEmpty.signal();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecutionException(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean trySend(Message<T> message) throws ExecutionException {
        if (message.isCloseMessage()) {
            close();
            return true;
        }
        lock.lock();
        try {
            if (closed) {
                throw new ExecutionException("Channel is closed");
            }
            if (queue.size() >= capacity) {
                return false;
            }
            queue.add(message);
            notEmpty.signal();
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean trySend(Message<T> message, long time, TimeUnit unit) throws ExecutionException {
        if (message.isCloseMessage()) {
            close();
            return true;
        }
        lock.lock();
        try {
            long nanos = unit.toNanos(time);
            while (!closed && queue.size() >= capacity) {
                if (nanos <= 0) {
                    return false;
                }
                nanos = notFull.awaitNanos(nanos);
            }
            if (closed) {
                throw new ExecutionException("Channel is closed");
            }
            queue.add(message);
            notEmpty.signal();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecutionException(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Message<T> receive() throws ExecutionException {
        lock.lock();
        try {
            while (queue.isEmpty() && !closed) {
                notEmpty.await();
            }
            if (queue.isEmpty()) {
                // closed and drained
                return Message.closed();
            }
            final var msg = queue.poll();
            notFull.signal();
            return msg;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecutionException(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Message<T> tryReceive() throws ExecutionException {
        lock.lock();
        try {
            if (queue.isEmpty()) {
                return closed ? Message.closed() : Message.empty();
            }
            final var msg = queue.poll();
            notFull.signal();
            return msg;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Message<T> tryReceive(long time, TimeUnit unit) throws ExecutionException {
        lock.lock();
        try {
            long nanos = unit.toNanos(time);
            while (queue.isEmpty() && !closed) {
                if (nanos <= 0) {
                    return Message.empty();
                }
                nanos = notEmpty.awaitNanos(nanos);
            }
            if (queue.isEmpty()) {
                // closed and drained
                return Message.closed();
            }
            final var msg = queue.poll();
            notFull.signal();
            return msg;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecutionException(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        lock.lock();
        try {
            closed = true;
            notFull.signalAll();  // blocked senders wake up and throw
            notEmpty.signalAll(); // blocked receivers wake up and get the close message
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }
}
