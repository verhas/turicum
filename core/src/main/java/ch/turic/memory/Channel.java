package ch.turic.memory;

import ch.turic.ExecutionException;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

/**
 * A channel can transfer messages (objects) between two asynchronous threads.
 */
public interface Channel<T> extends AutoCloseable, Iterable<T> {

    /**
     * A message that you can send through the channel. A message is simply an object wrapped with a flag to signal
     * the non-existence of the message, or empty message. The underlying JVM queues signal non-availability using null.
     * We, however, want to send null as an object. Thus, we send messages, and if the message is valid, not empty,
     * it still may contain null.
     * <p>
     * This is different from optional, which aims to avoid null check mistakes.
     */
    interface Message<T> {
        enum Type {
            OBJECT, // a plain old non-null object is in the message
            NULL, // a null object is in the message
            END, // this message signals the end of the message stream, no more messages will come
            EXCEPTION, // the message contains an exception, it will be thrown when fetched
        }

        /**
         * The message is empty. This type of message is not supposed to be sent through the channel.
         * This is to return when we try to read from a channel and there is no available message.
         *
         * @return {@code true} if the message is an empty message
         */
        boolean isEmpty();

        /**
         * Opposite of {@link #isEmpty()}
         *
         * @return {@code false} if the message is an empty message
         */
        default boolean isPresent() {
            return !isEmpty();
        }

        /**
         * This message is sent through the channel as a last message signaling that there are no more messages after
         * this message.
         *
         * @return {@code true} if this message is a close message.
         */
        boolean isCloseMessage();

        /**
         * The message contains an exception and not as an ordinary object but because an exception happened executing
         * the code of the future.
         *
         * @return {@code true} if there is an exception in the message
         */
        boolean isException();

        /**
         * Get the object wrapped into the message.
         *
         * @return the object that is wrapped into the message. It will throw {@link NoSuchElementException} if this is
         * a close message and an {@link ExecutionException} if the message is an exception message.
         */
        T get();

        /**
         * Get the object wrapped into the message even if it is an exception or a close message.
         *
         * @return the message and it does not throw it as an exception
         */
        T _get();

        /**
         * Create a normal object message
         *
         * @param value the object wrapped into the message
         * @param <T>   the type of the object
         * @return the created message
         */
        static <T> Message<T> of(T value) {
            return new SimpleMessage<>(value, Type.OBJECT);
        }

        /**
         * Create an empty message. Since empty messages are not sent through the channel, this method will mainly used
         * by the channel implementation to create an empty message when an underlying channel has nothing to deliver.
         *
         * @param <T> the type of the object
         * @return an empty message
         */
        static <T> Message<T> empty() {
            return new SimpleMessage<>(null, Type.NULL);
        }

        /**
         * Create a closed message. This method is called when a channel is closed.
         *
         * @param <T> the type of the messages
         * @return the closed message
         */
        static <T> Message<T> closed() {
            return new SimpleMessage<>(null, Type.END);
        }

        static Message<LngException> exception(LngException throwable) {
            return new SimpleMessage<>(throwable, Type.EXCEPTION);
        }
    }

    class SimpleMessage<T> implements Message<T> {
        final Type type;
        final T value;

        private SimpleMessage(T value, Type type) {
            this.type = type;
            this.value = value;
        }

        @Override
        public boolean isEmpty() {
            return type == Type.NULL;
        }

        @Override
        public boolean isException() {
            return type == Type.EXCEPTION;
        }

        @Override
        public boolean isCloseMessage() {
            return type == Type.END;
        }

        @Override
        public T _get() throws ExecutionException {
            return switch (type) {
                case NULL -> null;
                default -> value;
            };
        }

        @Override
        public T get() throws ExecutionException {
            return switch (type) {
                case OBJECT -> value;
                case NULL -> null;
                case END -> throw new NoSuchElementException();
                case EXCEPTION -> {
                    if (value instanceof Throwable) {
                        throw new ExecutionException((Throwable) value);
                    } else if (value instanceof LngException lngException) {
                        throw new ExecutionException(lngException.getCause());
                    }else{
                        throw new ExecutionException("Unkown message wrapped for exception '%s'",value);
                    }
                }
            };
        }
    }

    @Override
    default java.util.Iterator<T> iterator() {
        return new ChannelIterator<>(this);
    }

    @Override
    default void close() {
        send(Message.closed());
    }

    /**
     * Send a message to the receiving end. If the channel is full, then wait.
     * <p>
     * Note that it is absolutely valid to send a {@code null} as a message.
     *
     * @param message the message that is sent to the receiver
     * @throws ExecutionException can just happen anywhere in Turicum
     */
    void send(Message<T> message) throws ExecutionException;

    /**
     * Try to send a message to the receiving end and return {@code true} if the sending was successful
     * Return {@code false} if we cannot send a message at the moment because the channel is full.
     *
     * @param message the message to be sent
     * @return {@code true} if the message was sent
     * @throws ExecutionException can just happen anywhere in Turicum
     */
    boolean trySend(Message<T> message) throws ExecutionException;


    /**
     * Try to send a message to the receiver and wait for the channel to have free capacity.
     *
     * @param message the message to be sent
     * @param time    the time-out amount
     * @param unit    the time unit
     * @return {@code true} if the message was sent
     * @throws ExecutionException can just happen anywhere in Turicum
     */
    boolean trySend(Message<T> message, long time, TimeUnit unit) throws ExecutionException;

    /**
     * Receive a message from the channel. Wait until a message is available.
     *
     * @return the received message
     * @throws ExecutionException can just happen anywhere in Turicum, but also if there was a special message
     */
    Message<T> receive() throws ExecutionException;

    /**
     * Try to receive a message from the channel. If there is no message to receive, then return an empty message.
     *
     * @return the received message or an empty message
     * @throws ExecutionException can just happen anywhere in Turicum
     */
    Message<T> tryReceive() throws ExecutionException;

    /**
     * Try to receive a message from the channel.
     *
     * @param time the time-out amount
     * @param unit the time unit
     * @return {@code true} if the message was sent
     * @throws ExecutionException can just happen anywhere in Turicum
     */
    Message<T> tryReceive(long time, TimeUnit unit) throws ExecutionException;

    /**
     * Returns true of the channel is closed and cannot deliver more messages.
     * <p>
     * It can still contains some messages that you can receive.
     *
     * @return {@code true} if the channel is closed.
     */
    boolean isClosed();
}
