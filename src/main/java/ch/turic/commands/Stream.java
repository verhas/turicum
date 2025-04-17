package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.BlockingQueueYielder;
import ch.turic.memory.Context;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Stream extends AbstractCommand {
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Command command;
    private final Command capacityCommand;

    public Stream(Command command, Command capacityCommand) {
        this.command = command;
        this.capacityCommand = capacityCommand;
    }

    @Override
    public Object _execute(Context ctx) throws ExecutionException {

        if (!(ctx instanceof ch.turic.memory.Context context)) {
            throw new ExecutionException("Wrong context type, internal error");
        }

        final int capacity;
        if( capacityCommand == null ) {
            capacity = Integer.MAX_VALUE;
        }else{
            final var capacityObject  = capacityCommand.execute(ctx);
            if(Cast.isLong( capacityObject ) ) {
                capacity = Cast.toLong(capacityObject).intValue();
            }else{
                throw new ExecutionException("Invalid stream capacity value '%s', number is needed", capacityObject);
            }
        }
        context.globalContext.startMultithreading();
        final var newThreadContext = context.thread();

        copyVariables(context, newThreadContext);

        final var streamYielder = new BlockingQueueYielder(capacity);
        newThreadContext.threadContext.addYielder(streamYielder);
        CompletableFuture<Object> future =
                CompletableFuture.supplyAsync(() -> {
                    Thread.currentThread().setName("stream-thread");
                    try (streamYielder) {
                        command.execute(newThreadContext);
                    } catch (Exception t) {
                        return t;
                    }
                    return null;// no exception
                }, executor);
        streamYielder.setDataSource(future);
        return streamYielder;
    }

    private static void copyVariables(Context source, Context target) {
        for (final var key : source.keys()) {
            target.let0(key, source.get(key));
            target.freeze(key);
        }
    }
}

