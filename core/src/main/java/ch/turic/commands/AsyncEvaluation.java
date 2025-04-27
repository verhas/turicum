package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.AsyncStreamHandler;
import ch.turic.memory.Channel;
import ch.turic.memory.Context;
import ch.turic.memory.LngException;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncEvaluation extends AbstractCommand {
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Command command;
    private final Map<String, Command> options;

    public AsyncEvaluation(Command command, Map<String, Command> options) {
        this.command = command;
        this.options = options;
    }

    @Override
    public Object _execute(Context ctx) throws ExecutionException {
        int inCapacity = Integer.MAX_VALUE, outCapacity = Integer.MAX_VALUE, stepLimit = -1;
        long timeLimit = -1;
        for (final var key : options.keySet()) {
            switch (key) {
                case "in":
                    inCapacity = parameter(key, ctx, options.get(key), 1).intValue();
                    break;
                case "out":
                    outCapacity = parameter(key, ctx, options.get(key), 1).intValue();
                    break;
                case "steps":
                    stepLimit = parameter(key, ctx, options.get(key), 1).intValue();
                    break;
                case "time":
                    timeLimit = parameter(key, ctx, options.get(key), 1000);
                    break;
                default:
                    throw new ExecutionException("Unknown option: " + key);
            }
        }

        ctx.globalContext.startMultithreading();
        final var newThreadContext = ctx.thread();

        copyVariables(ctx, newThreadContext);
        final var yielder = new AsyncStreamHandler(outCapacity, inCapacity);
        newThreadContext.threadContext.addYielder(yielder);
        CompletableFuture<Channel.Message<?>> future =
                CompletableFuture.supplyAsync(() -> {
                    Thread.currentThread().setName("async-thread");
                    try (yielder) {
                        return Channel.Message.of(command.execute(newThreadContext));
                    } catch (Exception t) {
                        final var exception = LngException.build(ctx,t,newThreadContext.threadContext.getStackTrace());
                        return Channel.Message.exception(exception);
                    }
                }, executor);
        yielder.setFuture(future);
        return yielder;
    }

    private static void copyVariables(Context source, Context target) {
        for (final var key : source.keys()) {
            target.let0(key, source.get(key));
            target.freeze(key);
        }
    }

    private static Long parameter(String key, Context context, Command command, long multiplier) {
        final var arg = command.execute(context);
        if (Cast.isLong(arg)) {
            return Cast.toLong(arg) * multiplier;
        } else if (Cast.isDouble(arg)) {
            return Cast.toLong(Cast.toDouble(arg) * multiplier);
        } else {
            throw new ExecutionException("parameter %s='%s' for async is not valid", key, arg);
        }

    }

}

