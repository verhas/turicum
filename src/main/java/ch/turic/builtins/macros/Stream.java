package ch.turic.builtins.macros;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriMacro;
import ch.turic.commands.Command;
import ch.turic.memory.BlockingQueueYielder;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Stream implements TuriMacro {


    @Override
    public String name() {
        return "stream";
    }

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        if (!(ctx instanceof ch.turic.memory.Context context)) {
            throw new ExecutionException("Wrong context type, internal error");
        }
        if (arguments.length < 1) {
            throw new ExecutionException("Wrong number of arguments, expected 1 for async");
        }
        if (!(arguments[0] instanceof Command command)) {
            throw new ExecutionException("Wrong argument type, expected Command");
        }

        context.globalContext.startMultithreading();
        final var newThreadContext = context.thread();

        ContextCopy.copyVariables(context, newThreadContext);

        final var streamYielder = new BlockingQueueYielder();
        newThreadContext.threadContext.addYielder(streamYielder);
        CompletableFuture<Object> future =
                CompletableFuture.supplyAsync(() -> {
                    Thread.currentThread().setName("stream-thread");
                    try (streamYielder) {
                        command.execute(newThreadContext);
                    }catch (Exception t){
                        return t;
                    }
                    return null;// no exception
                }, executor);
        streamYielder.setDataSource(future);
        return streamYielder;
    }

}
