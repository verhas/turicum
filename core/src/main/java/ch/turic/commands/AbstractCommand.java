package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.analyzer.Pos;
import ch.turic.memory.Context;
import ch.turic.memory.LngStackFrame;

public abstract class AbstractCommand implements Command {

    private Pos startPosition;
    private Pos endPosition;

    public Pos startPosition() {
        return startPosition;
    }

    public Pos endPosition() {
        return endPosition;
    }

    public void setEndPosition(Pos endPosition) {
        this.endPosition = endPosition;
    }

    public void setStartPosition(Pos startPosition) {
        this.startPosition = startPosition;
    }

    public Object execute(final Context ctx) throws ExecutionException {
        final var sf = new LngStackFrame(this);
        ctx.threadContext.push(sf);
        final var result = _execute(ctx);
        ctx.threadContext.pop();
        return result;
    }

    public abstract Object _execute(final Context ctx) throws ExecutionException;
}
