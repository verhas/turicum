package ch.turic.memory.debugger;

import ch.turic.Command;
import ch.turic.commands.AbstractCommand;
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

    public void close() {
        pauseSignalChannel.close();
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

    public Set<BreakPoint> breakpoints() {
        return breakpoints;
    }

    public void addBreakPoint(BreakPoint bp) {
        //We want the new breakpoint in the list, and not two breakpoints with the same line number.
        breakpoints.remove(bp);
        breakpoints.add(bp);
    }

    private int suppressedFromLine = -1;
    private int suppressedToLine = -1;

    /**
     * Suppresses breakpoint pausing while the execution stays within the line span of the
     * command the debugger resumes from. Without this, a RUN issued while paused at a
     * breakpoint immediately re-pauses on the next node of the same source line (a line
     * consists of several command nodes, and the breakpoint matches all of them), so the
     * program appears not to resume at all. The suppression is cleared by the first command
     * whose span leaves the recorded one — in a loop, the next iteration re-arms the
     * breakpoint and it fires again.
     *
     * @param command the command the debugger is paused at when the RUN command is received
     */
    public void suppressBreaksAround(Command command) {
        if (command instanceof AbstractCommand ac && ac.startPosition() != null && ac.endPosition() != null) {
            suppressedFromLine = ac.startPosition().line;
            suppressedToLine = ac.endPosition().line;
        }
    }

    /**
     * Decides whether the running execution has to pause at this command because of a
     * breakpoint, honoring the post-resume suppression (see
     * {@link #suppressBreaksAround(Command)}).
     *
     * @param command the command about to execute
     * @return {@code true} if the execution has to pause
     */
    public boolean shouldBreak(Command command) {
        if (!(command instanceof AbstractCommand ac)) {
            return false;
        }
        if (ac.startPosition() == null || ac.endPosition() == null) {
            return false;
        }
        if (suppressedFromLine >= 0) {
            if (ac.startPosition().line <= suppressedToLine && ac.endPosition().line >= suppressedFromLine) {
                return false; // still inside the span the debugger resumed from
            }
            suppressedFromLine = -1;
            suppressedToLine = -1;
        }
        return isBreakPoint(command);
    }

    public boolean isBreakPoint(Command command) {
        if (command instanceof AbstractCommand abstractCommand) {
            if( abstractCommand.startPosition() == null || abstractCommand.endPosition() == null ) {
                return false;
            }
            // a breakpoint fires where a command STARTS on its line. Matching the whole
            // start..end span would fire on every ancestor node covering the line — a
            // breakpoint inside a loop body would stop at the loop header, and resuming
            // from there would have to suppress the whole loop span, silencing the
            // breakpoint for the rest of the loop.
            final var lineStart = abstractCommand.startPosition().line;
            return breakpoints.stream().anyMatch(bp -> bp.line == lineStart) ||
                    (parent != null && parent.isBreakPoint(abstractCommand));
        } else {
            return false;
        }
    }

    public void removeBreakPoint(BreakPoint bp) {
        breakpoints.remove(bp);
    }

    public DebuggerContext getParent() {
        return parent;
    }
}
