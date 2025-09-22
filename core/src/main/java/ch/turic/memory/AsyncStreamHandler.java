package ch.turic.memory;

import ch.turic.ExecutionException;
import ch.turic.builtins.classes.TuriMethod;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.*;

public class AsyncStreamHandler
        extends CompletableFuture<Object>
        implements Yielder, AutoCloseable, Iterable<Object>, Future<Object>, HasFields {

    private final BlockingQueueChannel<Object> toChildQueue;
    private final BlockingQueueChannel<Object> toParentQueue;
    private final Iterator<Object> parentIterator;
    private String name;

    public CompletableFuture<Channel.Message<?>> future() {
        return future;
    }

    private CompletableFuture<Channel.Message<?>> future;
    private ThreadContext threadContext;

    public AsyncStreamHandler(int outQueueSize, int inQueueSize) {
        this.toChildQueue = new BlockingQueueChannel<>(outQueueSize);
        this.toParentQueue = new BlockingQueueChannel<>(inQueueSize);
        this.parentIterator = toParentQueue.iterator();
        this.name = NameGen.generateName();
    }

    @Override
    public Iterator<Object> iterator() {
        return toParent().iterator();
    }

    @Override
    public Channel<Object> toChild() {
        return toChildQueue;
    }

    @Override
    public Channel<Object> toParent() {
        return toParentQueue;
    }

    @Override
    public void close() {
        toParent().close();
        toChild().close();
    }

    public void setFuture(CompletableFuture<Channel.Message<?>> future) {
        this.future = future;
    }

    public void setThreadContext(ThreadContext threadContext) {
        this.threadContext = threadContext;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }

    @Override
    public boolean isDone() {
        return future.isDone();
    }

    @Override
    public Channel.Message<?> get() throws ExecutionException {
        try {
            return future.get();
        } catch (CancellationException ce) {
            throw new ExecutionException("Task stopped.");
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public Channel.Message<?> get(long timeout, TimeUnit unit) throws ExecutionException {
        try {
            return future.get(timeout, unit);
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public void setField(String name, Object value) throws ExecutionException {
        throw new ExecutionException("You cannot set a field on an async object");
    }

    @Override
    public Object getField(String name) throws ch.turic.ExecutionException {
        return switch (name) {
            case "name" -> new TuriMethod<>(() -> this.name);
            case "is_done" -> new TuriMethod<>(future::isDone);
            case "is_cancelled" -> new TuriMethod<>(future::isCancelled);
            case "stop" -> new TuriMethod<>(x -> {
                future.cancel(true);
                threadContext.abort();
                toParent().close();
                toChild().close();
                return null;
            });
            case "is_err" -> new TuriMethod<>(() -> {
                try {
                    return future.isDone() && future.get().isException();
                } catch (Exception e) {
                    throw new ExecutionException(e);
                }
            });
            case "get_err" -> new TuriMethod<>(() -> {
                try {
                    if (future.isDone() && future.get().isException()) {
                        return future.get()._get();
                    }
                    throw new ExecutionException("get_err on a non erring task.");
                } catch (Exception e) {
                    throw new ExecutionException(e);
                }
            });
            case "get" -> new TuriMethod<>(() -> {
                try {
                    // get the message from the future and then get the value from the message or throw exception
                    // if it is an exception embedded in the message as an exception
                    return future.get().get();
                } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
                    throw new ExecutionException("Execution exception while waiting for thread %s", e.getMessage());
                }
            });
            case "close" -> new TuriMethod<>(() -> {
                toChild().close();
                return null;
            });
            case "send" -> new TuriMethod<>((args) -> {
                for (final var arg : args) {
                    toChild().send(Channel.Message.of(arg));
                }
                return null;
            });
            case "set_name" -> new TuriMethod<>((args) -> {
                for (final var arg : args) {
                    this.name = "" + arg;
                }
                return null;
            });
            case "has_next" -> new TuriMethod<>(parentIterator::hasNext);
            case "next" -> new TuriMethod<>(parentIterator::next);
            default -> throw new ExecutionException("Unknown method %s", name);
        };
    }

    @Override
    public Set<String> fields() {
        return Set.of();
    }
}
