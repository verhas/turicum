package ch.turic.builtins.macros;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriMacro;
import ch.turic.commands.Command;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ch.turic.builtins.macros.ContextCopy.copyVariables;

public class Async implements TuriMacro {
    @Override
    public String name() {
        return "async";
    }

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        if (!(ctx instanceof ch.turic.memory.Context context)) {
            throw new ExecutionException("Wrong context type, internal error");
        }
        if (arguments.length < 1) {
            throw new ExecutionException("Wrong number of arguments, expected at least 1 for async");
        }
        if (!(arguments[0] instanceof Command command)) {
            throw new ExecutionException("Wrong argument type, expected Command");
        }

        context.globalContext.startMultithreading();
        final var newThreadContext = context.thread();

        copyVariables( context, newThreadContext);

        return CompletableFuture.supplyAsync(() -> command.execute(newThreadContext), executor);
    }
}
