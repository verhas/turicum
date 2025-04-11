package javax0.turicum.commands;

import javax0.turicum.ExecutionException;
import javax0.turicum.memory.Context;

/**
 * Macro is similar to a closure, but it gets the arguments as {@link Command} objects and not evaluated.
 * After that the body of the lazy can decide to evaluate none, one, some or all of the arguments each one or more
 * times as it needs.
 *
 */
public final class Macro extends AbstractCommand implements ClosureOrMacro {
    final ParameterList parameters;
    final Context wrapped;
    final BlockCommand command;

    public BlockCommand command() {
        return command;
    }

    @Override
    public ParameterList parameters() {
        return parameters;
    }

    @Override
    public Context wrapped() {
        return wrapped;
    }

    public Macro(ParameterList parameters, Context wrapped, BlockCommand command) {
        this.parameters = parameters;
        this.wrapped = wrapped;
        this.command = command;
    }

    @Override
    public Object _execute(final Context ctx) throws ExecutionException {
        ctx.step();
        Object result = null;
        for (final var command : command.commands()) {
            ExecutionException.when(command instanceof BreakCommand, "You cannot break from a function or closure. Use Return");
            result = command.execute(ctx);
            if (result instanceof Conditional.ReturnResult returnResult && returnResult.isDone()) {
                return returnResult.result();
            }
        }
        return result;
    }
}
