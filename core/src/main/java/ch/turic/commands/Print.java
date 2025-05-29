package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.memory.Context;
import ch.turic.memory.LngObject;
import ch.turic.utils.Unmarshaller;

/**
 * Evaluate a Command
 */
public class Print extends AbstractCommand {

    // snipline PRINT_TARGET filter=.*"(.*?)"
    private static final String PRINT_TARGET = "print_target";
    // snipline PRINT_TARGET_WRITE filter=.*"(.*?)"
    private static final String WRITE = "write";
    // snipline PRINT_TARGET_FLUSH filter=.*"(.*?)"
    private static final String FLUSH = "flush";
    private final Command[] commands;
    private final boolean nl;

    public static Print factory(Unmarshaller.Args args) {
        return new Print(args.commands(), args.bool("nl"));
    }

    public Print(Command[] commands, boolean nl) {
        this.commands = commands;
        this.nl = nl;
    }

    private static void out(String str, Context ctx, Object outputHandle) {
        if (outputHandle == null) {
            System.out.print(str);
        } else {
            switch (outputHandle) {
                case LngObject outputHandlerObject -> {
                    final var entry = outputHandlerObject.getField(WRITE);
                    if (!(entry instanceof Closure closure)) {
                        throw new ExecutionException("output handler does not have '%s()' method", WRITE);
                    }
                    closure.callAsMethod(ctx, outputHandlerObject, WRITE, str);
                }
                case Closure closure -> closure.call(ctx, str);
                default -> throw new ExecutionException("unhandled output handle: " + outputHandle);
            }
        }
    }

    private static void flush(Context ctx, Object outputHandle) {
        if (outputHandle == null) {
            System.out.flush();
        }
        if (outputHandle instanceof LngObject outputHandleObject) {
            final var entry = outputHandleObject.getField(FLUSH);
            if (!(entry instanceof Closure closure)) {
                if (entry != null) {
                    throw new ExecutionException("output handler 'flush()' is not a method %s", entry);
                }
                // if it is not defined at all, that is okay
                return;
            }
            closure.callAsMethod(ctx, outputHandleObject, FLUSH);
        }
    }

    @Override
    public Object _execute(final Context ctx) throws ExecutionException {
        final var printTarget = ctx.contains(PRINT_TARGET) ? ctx.get(PRINT_TARGET) : null;
        if (printTarget != null && !(printTarget instanceof LngObject) && !(printTarget instanceof Closure)) {
            throw new ExecutionException("Output must be an object or closure");
        }
        for (final var cmd : commands) {
            final var arg = cmd.execute(ctx);
            final var str = switch (arg) {
                case null -> "none";
                default -> "" + arg;
            };
            out(str, ctx, printTarget);
        }
        if (nl) {
            out("\n", ctx, printTarget);
        }
        flush(ctx, printTarget);
        return null;
    }
}
