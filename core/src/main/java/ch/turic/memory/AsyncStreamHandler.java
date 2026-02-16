package ch.turic.memory;

import ch.turic.builtins.classes.TuriMethod;
import ch.turic.exceptions.ExecutionException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
    private LocalContext context;

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

    public void setContext(LocalContext context) {
        this.context = context;
        this.threadContext = context.threadContext;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        threadContext.abort();
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
    public Object getField(String name) throws ExecutionException {
        if (!getFieldMap().containsKey(name)) {
            throw new ExecutionException("Unknown field: " + name);
        }
        return getFieldMap().get(name);
    }

    private volatile Map<String, Object> fieldMap = null;

    private Map<String, Object> getFieldMap() {
        if (fieldMap == null) {
            synchronized (this) {
                if (fieldMap == null) {
                    fieldMap = new HashMap<>();

                    fieldMap.put("name", new TuriMethod<>(() -> this.name));
                    fieldMap.put("is_done", new TuriMethod<>(future::isDone));
                    fieldMap.put("is_cancelled", new TuriMethod<>(future::isCancelled));
                    fieldMap.put("stop", new TuriMethod<>(x -> {
                        future.cancel(true);
                        threadContext.abort();
                        toParent().close();
                        toChild().close();
                        return null;
                    }));
                    fieldMap.put("is_err", new TuriMethod<>(() -> {
                        try {
                            if (!future.isDone()) {
                                return false;
                            }
                            if (future.isCancelled() || future.isCompletedExceptionally()) {
                                return true;
                            }
                            try {
                                return future.get().isException();
                            } catch (Exception e) {
                                // Defensive: if get() still fails for any reason, treat as error.
                                return true;
                            }
                        } catch (Exception e) {
                            throw new ExecutionException(e);
                        }
                    }));
                    fieldMap.put("is_timeout", new TuriMethod<>(() -> {
                        if (!future.isDone()) {
                            return false;
                        }
                        try {
                            // Normal completion => not a timeout (even if the returned Channel.Message represents an error)
                            future.get();
                            return false;
                        } catch (java.util.concurrent.ExecutionException ee) {
                            return ee.getCause() instanceof TimeoutException;
                        } catch (CancellationException ce) {
                            return false;
                        } catch (Exception e) {
                            // Defensive: unknown failure mode => not classified as a timeout
                            return false;
                        }
                    }));
                    fieldMap.put("get_err", new TuriMethod<>(() -> {
                        if (!future.isDone()) {
                            throw new ExecutionException("get_err on a task that is not finished.");
                        }
                        try {
                            // Normal completion path: Channel.Message may itself represent an exception
                            final var msg = future.get();
                            if (msg != null && msg.isException()) {
                                return msg._get();
                            }
                            throw new ExecutionException("get_err on a non erring task.");
                        } catch (java.util.concurrent.ExecutionException ee) {
                            // Future completed exceptionally (e.g., timeout). Expose the cause.
                            return LngException.build(context, ee.getCause(), threadContext);
                        } catch (CancellationException ce) {
                            return ce;
                        } catch (Exception e) {
                            throw new ExecutionException(e);
                        }
                    }));
                    fieldMap.put("get", new TuriMethod<>(() -> {
                        try {
                            // get the message from the future and then get the value from the message or throw exception
                            // if it is an exception embedded in the message as an exception
                            return future.get().get();
                        } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
                            throw new ExecutionException("Execution exception while waiting for thread %s", e.getMessage());
                        }
                    }));
                    fieldMap.put("close", new TuriMethod<>(() -> {
                        toChild().close();
                        return null;
                    }));
                    fieldMap.put("send", new TuriMethod<>((args) -> {
                        for (final var arg : args) {
                            toChild().send(Channel.Message.of(arg));
                        }
                        return null;
                    }));
                    fieldMap.put("set_name", new TuriMethod<>((args) -> {
                        for (final var arg : args) {
                            this.name = "" + arg;
                        }
                        return null;
                    }));
                    fieldMap.put("has_next", new TuriMethod<>(parentIterator::hasNext));
                    fieldMap.put("next", new TuriMethod<>(parentIterator::next));
                }
            }
        }
        return fieldMap;
    }

    @Override
    public Set<String> fields() {
        return getFieldMap().keySet();
    }
}
