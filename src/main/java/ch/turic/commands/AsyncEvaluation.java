package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.memory.Context;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncEvaluation extends AbstractCommand {
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Command command;

    public AsyncEvaluation(Command command) {
        this.command = command;
    }

    @Override
    public Object _execute(Context ctx) throws ExecutionException {

        if (!(ctx instanceof ch.turic.memory.Context context)) {
            throw new ExecutionException("Wrong context type, internal error");
        }
        context.globalContext.startMultithreading();
        final var newThreadContext = context.thread();

        copyVariables(context, newThreadContext);

        return CompletableFuture.supplyAsync(() -> command.execute(newThreadContext), executor);
    }

    private static void copyVariables(Context source, Context target) {
        for (final var key : source.keys()) {
            target.let0(key, source.get(key));
            target.freeze(key);
        }
    }
}

