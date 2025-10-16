package ch.turic.commands;

import ch.turic.Command;
import ch.turic.exceptions.ExecutionException;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.*;
import ch.turic.utils.Unmarshaller;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncEvaluation extends AbstractCommand {
    private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Command command;
    private final Map<String, Command> options;

    public static AsyncEvaluation factory(Unmarshaller.Args args) {
        return new AsyncEvaluation(args.command("command"), args.get("options", Map.class));
    }

    public AsyncEvaluation(Command command, Map<String, Command> options) {
        this.command = command;
        this.options = options;
    }

    @Override
    public Object _execute(LocalContext ctx) throws ExecutionException {
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

        ctx.globalContext.switchToMultithreading();

        if (command instanceof ListComposition composition) {
            final var lngList = new LngList();
            for (final var command : composition.array) {
                lngList.array.add(startAsyncStream(command, ctx, outCapacity, inCapacity));
            }
            return lngList;
        } else {
            return startAsyncStream(this.command, ctx, outCapacity, inCapacity);
        }
    }

    private AsyncStreamHandler startAsyncStream(Command command, LocalContext ctx, int outCapacity, int inCapacity) {
        final var yielder = new AsyncStreamHandler(outCapacity, inCapacity);

        final var newContext = ctx.thread();
        copyVariables(ctx, newContext);
        newContext.threadContext.addYielder(yielder);

        CompletableFuture<Channel.Message<?>> future =
                CompletableFuture.supplyAsync(() -> {
                    Thread.currentThread().setName(NameGen.generateName());
                    newContext.threadContext.setThread(Thread.currentThread());
                    try (yielder) {
                        return Channel.Message.of(command.execute(newContext));
                    } catch (Exception t) {
                        final var exception = LngException.build(ctx, t, newContext.threadContext.getStackTrace());
                        return Channel.Message.exception(exception);
                    } finally {
                        newContext.close();
                    }
                }, executor);
        yielder.setFuture(future);
        yielder.setThreadContext(newContext.threadContext);
        return yielder;
    }

    /**
     * Copy the variables from the source context to the target context and freeze them in the target context.
     * The target context cannot and must not change these variables. It may, however, create new variables that it
     * can change.
     *
     * @param source the context from which we copy the variables
     * @param target the context to which we copy the variables
     */
    private static void copyVariables(LocalContext source, LocalContext target) {
        for (final var key : source.keys()) {
            target.let0(key, source.get(key));
            target.freeze(key);
        }
    }

    private static Long parameter(String key, LocalContext context, Command command, long multiplier) {
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

