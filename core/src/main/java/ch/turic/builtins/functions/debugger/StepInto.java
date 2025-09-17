package ch.turic.builtins.functions.debugger;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.SnakeNamed.Name;
import ch.turic.TuriFunction;
import ch.turic.builtins.functions.FunUtils;
import ch.turic.memory.Channel;
import ch.turic.memory.debugger.ConcurrentWorkItem;
import ch.turic.memory.debugger.DebuggerCommand;

@Name("debug_step_into")
public class StepInto implements TuriFunction {
    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        final var arg = FunUtils.arg(name(), arguments);
        try {
            if (arg instanceof Channel<?>) {
                final var debuggerChannel = (Channel<ConcurrentWorkItem<DebuggerCommand>>) arg;
                final var message = debuggerChannel.receive();
                if (message.isCloseMessage() || message.isEmpty()) {
                    return null;
                }
                final var item = message.get();
                final var debuggerCommand = item.payload();
                debuggerCommand.setCommand(DebuggerCommand.Command.STEP_INTO);
                debuggerCommand.setRequestParameters(null);
                item.complete(debuggerCommand);
                return "OK";
            } else {
                throw new ExecutionException("debug_step_into expects a channel as argument");
            }
        } catch (Throwable e) {
            throw new ExecutionException(e, "Debugging was interrupted");
        }
    }
}
