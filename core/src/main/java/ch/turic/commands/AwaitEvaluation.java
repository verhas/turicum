package ch.turic.commands;

import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.AsyncStreamHandler;
import ch.turic.memory.Context;
import ch.turic.memory.LngList;
import ch.turic.utils.Unmarshaller;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class AwaitEvaluation extends AbstractCommand {
    private final Command command;
    private final Map<String, Command> options;


    public static AwaitEvaluation factory(final Unmarshaller.Args args) {
        return new AwaitEvaluation(
                args.command("command"),
                args.get("options", Map.class)
        );
    }

    public AwaitEvaluation(Command command, Map<String, Command> options) {
        this.command = command;
        this.options = options;
    }

    @Override
    public Object _execute(Context ctx) throws ExecutionException {

        long timeLimit = -1;
        for (final var key : options.keySet()) {
            timeLimit = switch (key) {
                case "time" -> parameter(key, ctx, options.get(key), 1000);
                default -> throw new ExecutionException("Unknown option: " + key);
            };
        }
        final Object result = command.execute(ctx);
        final Object future;
        if (result instanceof LngList) {
            final var futures = new ArrayList<CompletableFuture<?>>();
            for (final var f : (Iterable<?>) result) {
                if (f instanceof AsyncStreamHandler asyncStreamHandler) {
                    futures.add(asyncStreamHandler.future());
                } else {
                    throw new ExecutionException("Cannot wait on %s ", f);
                }
            }
            if (futures.isEmpty()) {
                return null;
            }
            future = CompletableFuture.anyOf(futures.toArray(CompletableFuture[]::new));
        } else {
            future = result;
        }

        if (future instanceof AsyncStreamHandler ash) {
            try {
                if (timeLimit == -1) {
                    return ash.get().get();
                }
                return ash.get(timeLimit, TimeUnit.MILLISECONDS).get();
            } catch (ExecutionException e) {
                throw e;
            } catch (Exception e) {
                throw new ExecutionException(e);
            }
        }
        if (future instanceof CompletableFuture<?> cf) {
            try {
                if (timeLimit == -1) {
                    return cf.get();
                }
                return cf.get(timeLimit, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                throw new ExecutionException(e);
            }
        }
        throw new ExecutionException("I cannot wait for %s", future);

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

