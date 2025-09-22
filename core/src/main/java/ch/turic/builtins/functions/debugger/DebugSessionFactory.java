package ch.turic.builtins.functions.debugger;

import ch.turic.*;
import ch.turic.SnakeNamed.Name;
import ch.turic.builtins.functions.FunUtils;
import ch.turic.memory.BlockingQueueChannel;
import ch.turic.memory.Channel;
import ch.turic.memory.ChannelIterator;
import ch.turic.memory.debugger.ConcurrentWorkItem;

import java.io.IOException;
import java.nio.file.Paths;

@Name("debug_session")
public class DebugSessionFactory implements TuriFunction {
    @Override
    public DebugSession call(Context ctx, Object[] arguments) throws ExecutionException {
        final var file = FunUtils.arg(name(), arguments, String.class);
        try {
            final var input = Input.fromFile(Paths.get(file));
            try (Interpreter interpreter = new Interpreter(input)) {
                final var program = interpreter.compile();
                final var debugQue = new ChannelIterator<>(new BlockingQueueChannel<ConcurrentWorkItem<?>>(1));
                final var session = new DebugSession(debugQue);
                interpreter.debugMode(true, debugQue);
                Thread.ofVirtual().start(() -> {
                    try {
                        Thread.currentThread().setName("DEBUGGED THREAD");
                        interpreter.executeNotify(program, session.finisher);
                    } catch (Throwable e) {
                        final var message = (Channel.Message<ConcurrentWorkItem<?>>) Channel.Message.exception(e);
                        debugQue.send(message);
                    }
                });
                return session;
            }
        } catch (IOException e) {
            throw new ExecutionException(e, "Cannot debug the file %2s",Paths.get(file).toAbsolutePath());
        }
    }
}
