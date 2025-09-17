package ch.turic.memory.debugger;

import ch.turic.memory.Channel;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class DebuggerContext {
    final private DebuggerContext parent;
    private final Channel<ConcurrentWorkItem<?>> pauseSignalChannel;

    /**
     * Constructs a new DebuggerContext instance.
     *
     * @param parent      the parent DebuggerContext, or null if this is the root context.
     *                    Each thread has its own DebuggerContext. The root context is the one
     *                    associated with the interpreter and the global context.
     * @param pauseSignal the communication channel used to send pause signals
     */
    public DebuggerContext(DebuggerContext parent, Channel<ConcurrentWorkItem<?>> pauseSignal) {
        this.parent = parent;
        this.pauseSignalChannel = pauseSignal;
    }

    public enum State {
        RUNNING,
        PAUSED,
        STEPPING
    }

    private final AtomicReference<State> state = new AtomicReference<>(State.STEPPING);

    public State getState() {
        return state.get();
    }

    public DebuggerCommand pause(DebuggerCommand command) throws Throwable {
        setState(State.PAUSED);
        final var item = ConcurrentWorkItem.of(command);
        pauseSignalChannel.send(Channel.Message.of(item));
        return item.await();
    }

    public void setState(State state) {
        this.state.set(state);
    }


    final Set<BreakPoint> breakpoints = new HashSet<>();

    public void addBreakPoint(BreakPoint bp) {
        //We want the new breakpoint in the list, and not two breakpoints with the same line number.
        breakpoints.remove(bp);
        breakpoints.add(bp);
    }

    public boolean isBreakPoint(int lineStart, int lineEnd) {
        return breakpoints.stream().anyMatch(bp -> bp.line >= lineStart && bp.line <= lineEnd) ||
                (parent != null && parent.isBreakPoint(lineStart, lineEnd));
    }

    public void removeBreakPoint(BreakPoint bp) {
        breakpoints.remove(bp);
    }

    public DebuggerContext getParent() {
        return parent;
    }
}
