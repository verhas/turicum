package ch.turic.builtins.classes;

import ch.turic.ExecutionException;
import ch.turic.LngCallable;
import ch.turic.TuriClass;
import ch.turic.memory.Channel;
import ch.turic.memory.ChannelIterator;

import java.util.NoSuchElementException;

public class TuriChannel implements TuriClass {
    @Override
    public Class<?> forClass() {
        return ChannelIterator.class;
    }

    @Override
    public LngCallable getMethod(Object target, String identifier) {
        if (!(target instanceof ChannelIterator<?>)) {
            throw new ExecutionException("Target object is not a Channel, this is an internal error");
        }
        final var channelIterator = (ChannelIterator<Object>) target;
        return switch (identifier) {
            case "close" -> new TuriMethod<>(() -> {
                channelIterator.close();
                return null;
            });
            case "is_closed" -> new TuriMethod<>(channelIterator::isClosed);
            case "receive" -> new TuriMethod<>(() -> {
                try {
                    // get the message from the future and then get the value from the message or throw exception
                    // if it is an exception embedded in the message as an exception
                    return channelIterator.receive().get();
                } catch (Exception e) {
                    throw new ExecutionException("Execution exception while reading from queue.", e);
                }
            });
            case "try_receive" -> new TuriMethod<>(() -> {
                try {
                    final var msg = channelIterator.tryReceive();
                    if (msg == null) {
                        return null;
                    } else {
                        return msg.get();
                    }
                } catch (NoSuchElementException nse) {
                    // we just try to read, and the channel may not be closed when we checked but gets closed why we
                    // try to read. try_receive must be lenient
                    return null;
                } catch (Exception e) {
                    throw new ExecutionException("Execution exception while reading from queue.", e);
                }
            });
            case "send" -> new TuriMethod<>((sendArgs) -> {
                try {
                    channelIterator.send(Channel.Message.of(sendArgs[0]));
                    return true;
                } catch (Exception e) {
                    throw new ExecutionException("Execution exception while sending message to queue %s", e.getMessage());
                }
            });
            case "try_send" -> new TuriMethod<>((trySendArgs) -> {
                try {
                    return channelIterator.trySend(Channel.Message.of(trySendArgs[0]));
                } catch (Exception e) {
                    throw new ExecutionException("Execution exception while sending message to queue %s", e.getMessage());
                }
            });
            default -> throw new ExecutionException("Unknown method %s", identifier);
        };
    }
}
