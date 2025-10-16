package ch.turic.memory;

import ch.turic.exceptions.ExecutionException;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

/**
 * The {@code ChannelIterator} class acts as an iterator implementation over a {@link Channel}.
 * It enables iteration over the elements in a channel, while also delegating operations
 * to the underlying {@link Channel} instance.
 *
 * <p>This class implements both the {@link Channel} and {@link Iterator} interfaces.
 * The delegation methods handle standard channel operations such as sending, receiving, and closing,
 * while the iterator methods {@link #hasNext()} and {@link #next()} allow sequential access to the elements
 * within the channel.
 *
 * @param <T> the type of elements handled by the channel.
 */
public class ChannelIterator<T> implements Channel<T>, Iterator<T> {
    private final Channel<T> channel;

    public ChannelIterator(Channel<T> channel) {
        this.channel = channel;
    }

    //<editor-fold id="delegated methods>
    /*
        Methods that just delegate the call to the underlying channel.
        This is the way this class implements the Channel interface.
     */
    @Override
    public void send(Message<T> message) throws ExecutionException {
        channel.send(message);
    }

    @Override
    public boolean trySend(Message<T> message) throws ExecutionException {
        return channel.trySend(message);
    }

    @Override
    public boolean trySend(Message<T> message, long time, TimeUnit unit) throws ExecutionException {
        return channel.trySend(message, time, unit);
    }

    @Override
    public Message<T> receive() throws ExecutionException {
        return channel.receive();
    }

    @Override
    public Message<T> tryReceive() throws ExecutionException {
        return channel.tryReceive();
    }

    @Override
    public Message<T> tryReceive(long time, TimeUnit unit) throws ExecutionException {
        return channel.tryReceive(time, unit);
    }

    @Override
    public void close() {
        channel.close();
    }

    @Override
    public boolean isClosed() {
        return channel.isClosed();
    }
    //</editor-fold id="delegated methods>

    private Message<T> nextItem;
    private boolean hasNextMessage = true;
    private boolean nextCached = false;

    /**
     * Query {@code hasNext()} to know if there is a next member in the channel. This method is part of the
     * {@link Iterator} implementation of this interface.
     * <p>
     * We have to use two boolean variables to remember the state of the reading ahead.
     * <p>
     * {@code nextCached} is {@code true} if we have read the next element alread from the channel.
     * However, it does not mean that there is a next element.
     * It can be a close message signaling that the channel is closing, or it can be an exception message.
     * If the message is a close message then {@code hasNextMessage} will be {@code false} and the method will return
     * false.
     * However, if the message cached contains an exception, the return value will still be {@code true} and when the
     * message is fetched calling {@link #next()} the exception will be thrown. That way one end of the communication
     * can transfer an exception through the channel.
     *
     * @return {@code true} if the channel has next element.
     */
    @Override
    public boolean hasNext() {
        if (nextCached) {
            return hasNextMessage;
        }
        nextItem = channel.receive();
        hasNextMessage = !nextItem.isCloseMessage();
        nextCached = true;
        return hasNextMessage;
    }

    /**
     * Get the next message from the channel. Thow exception if there are no more elements or when the element is an
     * exception.
     * <p>
     * Note that the channels contain messages wrapped around objects. This method returns the object and not the
     * message.
     *
     * @return the next element.
     */
    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException("Channel has exhausted");
        }
        nextCached = false;
        return nextItem.get();
    }
}
