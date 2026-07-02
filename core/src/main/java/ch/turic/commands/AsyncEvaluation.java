package ch.turic.commands;

import ch.turic.Command;
import ch.turic.commands.operators.Cast;
import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.*;
import ch.turic.utils.Unmarshaller;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AsyncEvaluation extends AbstractCommand {
    private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Command command;
    private final Map<String, Command> options;

    public static AsyncEvaluation factory(Unmarshaller.Args args) {
        final var command = args.command("command");
        @SuppressWarnings("unchecked")
        final Map<String,Command> options = args.get("options", Map.class);
        return new AsyncEvaluation(command, options);
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
                lngList.array.add(startAsyncStream(command, ctx, outCapacity, inCapacity, stepLimit, timeLimit));
            }
            return lngList;
        } else {
            return startAsyncStream(this.command, ctx, outCapacity, inCapacity, stepLimit, timeLimit);
        }
    }

    /**
     * Starts a new asynchronous evaluation “stream” for the given {@code command} and returns an
     * {@link AsyncStreamHandler} that acts as both:
     *
     * <ul>
     *   <li>a <em>controller</em> for the running task (via the underlying {@link CompletableFuture}), and</li>
     *   <li>a <em>bridge</em> for bidirectional message passing between the parent thread and the child thread.</li>
     * </ul>
     *
     * <h2>What this method does</h2>
     * <ol>
     *   <li>Creates an {@link AsyncStreamHandler} with bounded queues sized by {@code outCapacity} and {@code inCapacity}.</li>
     *   <li>Creates a new thread-specific {@link LocalContext} from {@code ctx} and copies (then freezes) the current
     *       variables into it, ensuring the child sees a consistent snapshot of the parent’s variables.</li>
     *   <li>Registers the created handler as a yielder in the new thread context.</li>
     *   <li>Schedules the {@code command} for execution using the class-level {@code executor} (virtual threads),
     *       wiring the outcome into a {@code CompletableFuture}.</li>
     * </ol>
     *
     * <h2>Result and error propagation</h2>
     * <p>
     * The command’s return value is wrapped into a {@code Channel.Message} and stored as the completion value of the
     * handler’s internal future. If execution throws, the exception is converted into a language-specific exception
     * (including stack trace information from the new thread context) and wrapped as an exception message instead.
     * </p>
     *
     * <h2>Resource and lifecycle notes</h2>
     * <ul>
     *   <li>The handler is used in a try-with-resources block in the child task; upon completion it is closed, which closes
     *       the underlying channels.</li>
     *   <li>The newly created {@link LocalContext} is always closed at the end of execution (success or failure).</li>
     *   <li>The returned {@link AsyncStreamHandler} exposes cancellation; canceling the handler cancels the underlying
     *       asynchronous computation.</li>
     * </ul>
     *
     * <h2>Limits</h2>
     * <p>
     *
     * @param command     the command to execute asynchronously in the child thread context
     * @param ctx         the parent context used as the source for creating and initializing the child thread context
     * @param outCapacity the capacity of the channel/queue used to send messages from the parent to the child
     * @param inCapacity  the capacity of the channel/queue used to send messages from the child to the parent
     * @param stepLimit   maximum allowed evaluation steps for the child execution; a negative value typically means “unlimited”
     * @param timeLimit   maximum allowed runtime for the child execution (interpretation depends on the surrounding implementation);
     *                    a negative value typically means “unlimited”
     * @return an {@link AsyncStreamHandler} connected to the scheduled task and initialized with the child thread context;
     * it can be used to exchange messages and to await/cancel completion
     */
    private AsyncStreamHandler startAsyncStream(Command command,
                                                LocalContext ctx,
                                                int outCapacity,
                                                int inCapacity,
                                                int stepLimit,
                                                long timeLimit) {
        final var yielder = new AsyncStreamHandler(outCapacity, inCapacity);

        final var newContext = ctx.thread();
        copyVariables(ctx, newContext);
        if (stepLimit >= 0) {
            newContext.threadContext.setStepLimit(stepLimit);
        }
        newContext.threadContext.addYielder(yielder);

        CompletableFuture<Channel.Message<?>> future =
                CompletableFuture.supplyAsync(() -> {
                    Thread.currentThread().setName(NameGen.generateName());
                    newContext.threadContext.setThread(Thread.currentThread());
                    try (yielder) {
                        return Channel.Message.of(command.execute(newContext));
                    } catch (Exception t) {
                        final var exception = LngException.build(ctx, t, newContext.threadContext);
                        return Channel.Message.exception(exception);
                    } finally {
                        newContext.close();
                    }
                }, executor);
        if (timeLimit >= 0) {
            future = future.orTimeout(timeLimit, TimeUnit.MILLISECONDS);
            // the timeout only completes the future; the interpreter thread must also be stopped
            future.whenComplete((result, throwable) -> {
                if (throwable instanceof TimeoutException) {
                    newContext.threadContext.abort();
                }
            });
        }
        yielder.setFuture(future);
        yielder.setContext(newContext);
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
        for (final var key : source.allLocalKeys()) {
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

