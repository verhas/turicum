package ch.turic.builtins.functions.debugger;

import ch.turic.Command;
import ch.turic.exceptions.ExecutionException;
import ch.turic.analyzer.Pos;
import ch.turic.memory.Channel;
import ch.turic.memory.LngList;
import ch.turic.memory.ThreadContext;
import ch.turic.memory.debugger.ConcurrentWorkItem;
import ch.turic.memory.debugger.DebuggerCommand;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a debugging session that tracks and manages work items related to
 * concurrent debugging operations. The `DebugSession` provides functionality to
 * organize, query, and manage work items and their associated thread contexts.
 */
public class DebugSession {
    private final Channel<ConcurrentWorkItem<?>> channel;

    private final AtomicBoolean finished = new AtomicBoolean(false);
    public final Runnable finisher = () -> finished.set(true);

    public DebugSession(Channel<ConcurrentWorkItem<?>> channel) {
        this.channel = channel;
    }

    private static class ThreadState {
        DebuggerCommand.PosResponse posResponse;
        DebuggerCommand.VarResponse localVarResponse;
        DebuggerCommand.VarResponse globalVarResponse;
        DebuggerCommand.CommandResponse commandResponse;
        DebuggerCommand.BreakpointsResponse breakpointsResponse;
    }

    private final Set<ConcurrentWorkItem<?>> workItems = new HashSet<>();
    private final Map<String, ThreadState> threadStates = new HashMap<>();

    /**
     * Fills the internal work item set (`workItems`) with messages received from the associated channel.
     * <p>
     * The method continuously attempts to receive messages from the channel using `tryReceive`.
     * If a non-empty message is received, the object wrapped in the message is added to the `workItems` set.
     * This process continues until an empty message is encountered, at which point the method terminates.
     * <p>
     * This method interacts with the channel associated with the `DebugSession` to retrieve `ConcurrentWorkItem` objects,
     * which are stored for potential debugging or processing purposes.
     * <p>
     * Throws:
     * - `ExecutionException` if the channel encounters an error while attempting to receive messages.
     */
    private void fillUpWorkItemTable() {
        var msg = channel.tryReceive();
        while (!msg.isEmpty()) {
            final var wi = (ConcurrentWorkItem<DebuggerCommand>) msg.get();
            final var dc = wi.payload();
            final var threadName = dc.threadContext().getThread().getName();
            switch (dc.command()) {
                case POS -> {
                    getThreadState(threadName).posResponse = (DebuggerCommand.PosResponse) dc.response;
                }
                case LOCALS -> {
                    getThreadState(threadName).localVarResponse = (DebuggerCommand.VarResponse) dc.response;
                }
                case GLOBALS -> {
                    getThreadState(threadName).globalVarResponse = (DebuggerCommand.VarResponse) dc.response;
                }
                case COMMAND -> {
                    getThreadState(threadName).commandResponse = (DebuggerCommand.CommandResponse) dc.response;
                }
                case BREAKPOINTS -> {
                    getThreadState(threadName).breakpointsResponse = (DebuggerCommand.BreakpointsResponse) dc.response;
                }
                case GLOBAL_BREAKPOINTS -> {
                    getThreadState(threadName).breakpointsResponse = (DebuggerCommand.BreakpointsResponse) dc.response;
                }
                case null, default -> {
                }
            }
            workItems.add(wi);
            msg = channel.tryReceive();
        }
    }

    private ThreadState getThreadState(String threadName) {
        return threadStates.computeIfAbsent(threadName, x -> new ThreadState());
    }

    /**
     * Retrieves a list of thread names from the current set of work items.
     * The method processes the internal work item table by extracting the thread
     * names from each associated thread context. The resulting list of thread
     * names is returned as an `LngList` object.
     *
     * @return A synchronized list of thread names extracted from the
     * thread contexts of the associated work items.
     */
    public synchronized LngList threads() {
        fillUpWorkItemTable();
        final var retval = new LngList();
        retval.array.addAll(workItems.stream().map(ConcurrentWorkItem::payload)
                .map(x -> (DebuggerCommand) x)
                .map(DebuggerCommand::threadContext)
                .map(ThreadContext::getThread)
                .map(Thread::getName)
                .toList());
        return retval;
    }

    private String debuggedThreadName = null;

    /**
     * Sets the thread identified by the given thread name as the current debugged thread context.
     * This method inspects the work items available in the session, searching for a thread context
     * associated with a thread that matches the provided name. The matching thread context is set
     * as the current debugged thread context.
     * <p>
     * The method synchronizes access to ensure thread safety while modifying the debugged thread context.
     *
     * @param threadName The name of the thread to be set as the current debugged thread context.
     *                   If no thread with the specified name is found, the debugged thread context
     *                   will be set to null.
     */
    public synchronized void set_thread(final String threadName) {
        debuggedThreadName = threadName;
    }

    /**
     * Retrieves the name of the thread associated with the current debugged thread context.
     * If the debugged thread context is null, the method will return null.
     *
     * @return The name of the thread in the current debugged thread context,
     * or null if the debugged thread context is not set.
     */
    public synchronized String thread() {
        return debuggedThreadName;
    }

    /**
     * Determines if the debug session has started by checking the presence of work items
     * in the internal work item set. The method fills the internal work item table
     * before performing the check.
     *
     * @return {@code true} if there are work items indicating the session has started;
     * {@code false} otherwise.
     */
    public synchronized boolean is_started() {
        fillUpWorkItemTable();
        return !workItems.isEmpty();
    }

    /**
     * Determines whether the debugged thread represented by the current debugged thread name
     * is paused by checking the work items queue for a matching thread context.
     *
     * @return {@code true} if a work item associated with the debugged thread exists, indicating
     * the thread is paused; {@code false} otherwise.
     */
    public synchronized boolean is_paused() {
        fillUpWorkItemTable();
        return workItems.stream()
                .anyMatch(w -> ((DebuggerCommand) w.payload()).threadContext().getThread().getName().equals(debuggedThreadName));
    }


    private ConcurrentWorkItem<DebuggerCommand> fetchWorkItem() {
        fillUpWorkItemTable();
        final var item = workItems.stream()
                .filter(w -> ((DebuggerCommand) w.payload()).threadContext().getThread().getName().equals(debuggedThreadName))
                .map(w -> (ConcurrentWorkItem<DebuggerCommand>) w)
                .findFirst().orElseThrow(() -> new ExecutionException("No debugged paused thread context for '" + debuggedThreadName + "'"));
        workItems.remove(item);
        return item;
    }

    public synchronized void stop() {
        final var wi = fetchWorkItem();
        wi.payload().setCommand(DebuggerCommand.Command.STOP);
        wi.complete();
    }

    public synchronized void step_over() {
        final var wi = fetchWorkItem();
        wi.payload().setCommand(DebuggerCommand.Command.STEP_OVER);
        wi.complete();
    }

    public synchronized void run() {
        final var wi = fetchWorkItem();
        wi.payload().setCommand(DebuggerCommand.Command.RUN);
        wi.complete();
    }

    public synchronized void step_into() {
        final var wi = fetchWorkItem();
        wi.payload().setCommand(DebuggerCommand.Command.STEP_INTO);
        wi.complete();
    }

    public synchronized void fetch_command() {
        final var wi = fetchWorkItem();
        wi.payload().setCommand(DebuggerCommand.Command.COMMAND);
        wi.complete();
    }

    public synchronized void fetch_breakpoints() {
        final var wi = fetchWorkItem();
        wi.payload().setCommand(DebuggerCommand.Command.BREAKPOINTS);
        wi.complete();
    }

    public synchronized void fetch_global_breakpoints() {
        final var wi = fetchWorkItem();
        wi.payload().setCommand(DebuggerCommand.Command.GLOBAL_BREAKPOINTS);
        wi.complete();
    }

    public synchronized void add_breakpoint(int line) {
        final var wi = fetchWorkItem();
        wi.payload().setCommand(DebuggerCommand.Command.ADD_BREAKPOINT);
        wi.payload().setBreakPointLine(line);
        wi.complete();
    }

    public synchronized void remove_breakpoint(int line) {
        final var wi = fetchWorkItem();
        wi.payload().setCommand(DebuggerCommand.Command.REMOVE_BREAKPOINT);
        wi.payload().setBreakPointLine(line);
        wi.complete();
    }

    public synchronized void add_global_breakpoint(int line) {
        final var wi = fetchWorkItem();
        wi.payload().setCommand(DebuggerCommand.Command.ADD_GLOBAL_BREAKPOINT);
        wi.payload().setBreakPointLine(line);
        wi.complete();
    }

    public synchronized void remove_global_breakpoint(int line) {
        final var wi = fetchWorkItem();
        wi.payload().setCommand(DebuggerCommand.Command.REMOVE_GLOBAL_BREAKPOINT);
        wi.payload().setBreakPointLine(line);
        wi.complete();
    }

    public synchronized void fetch_locals() {
        final var wi = fetchWorkItem();
        wi.payload().setCommand(DebuggerCommand.Command.LOCALS);
        wi.complete();
    }

    public synchronized void fetch_globals() {
        final var wi = fetchWorkItem();
        wi.payload().setCommand(DebuggerCommand.Command.GLOBALS);
        wi.complete();
    }

    public synchronized void fetch_pos() {
        final var wi = fetchWorkItem();
        wi.payload().setCommand(DebuggerCommand.Command.POS);
        wi.complete();
    }

    public synchronized Command command() {
        return getThreadState(debuggedThreadName).commandResponse.command();
    }

    public record Variable(String name, Object value) {
    }

    private LngList vars(DebuggerCommand.VarResponse vars) {
        final var retval = LngList.of();
        for (final var entry : vars.variables().entrySet()) {
            retval.array.add(new Variable(entry.getKey(), entry.getValue()));
        }
        return retval;
    }

    public synchronized LngList globals() {
        return vars(getThreadState(debuggedThreadName).globalVarResponse);
    }

    public synchronized LngList locals() {
        return vars(getThreadState(debuggedThreadName).localVarResponse);
    }

    public synchronized String command_str() {
        final var cmd = getThreadState(debuggedThreadName).commandResponse.command();
        return cmd.getClass().getSimpleName();
    }

    public synchronized LngList breakpoints() {
        final var command = getThreadState(debuggedThreadName);
        final var breakpoints = command.breakpointsResponse.breakPoints();
        final var retval = LngList.of();
        for (final var point : breakpoints) {
            retval.array.add((long) point.line);
        }
        return retval;
    }

    public synchronized Pos start_pos() {
        return getThreadState(debuggedThreadName).posResponse.startPosition();
    }

    public synchronized Pos end_pos() {
        return getThreadState(debuggedThreadName).posResponse.endPosition();
    }

    public synchronized boolean is_finished() {
        return finished.get();
    }

}
