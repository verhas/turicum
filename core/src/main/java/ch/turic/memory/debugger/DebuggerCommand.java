package ch.turic.memory.debugger;

import ch.turic.analyzer.Pos;
import ch.turic.memory.ThreadContext;

import java.util.Map;
import java.util.Set;

public class DebuggerCommand {
    public DebuggerCommand(ThreadContext threadContext) {
        this.threadContext = threadContext;
    }

    public enum Command {
        STEP_INTO,
        STEP_OVER,
        RUN,
        STOP,
        POS, // answer the position of the current execution point
        LOCALS, // list the local variable names
        GLOBALS, // list the global variable names
        SET, // set the value of a variable
        COMMAND, // get the current command
        BREAKPOINTS, // get the list of the thread-specific breakpoints
        GLOBAL_BREAKPOINTS, // get the list of the global breakpoints
        ADD_BREAKPOINT, // add a breakpoint affecting only this thread
        REMOVE_BREAKPOINT, // remove a breakpoint affecting only this thread
        ADD_GLOBAL_BREAKPOINT, // add a breakpoint affecting all threads
        REMOVE_GLOBAL_BREAKPOINT, // remove a breakpoint from the global set
    }

    private Command command;

    public int breakPointLine() {
        return breakPointLine;
    }

    public void setBreakPointLine(int breakPointLine) {
        this.breakPointLine = breakPointLine;
    }

    private int breakPointLine;

    public void setCommand(Command command) {
        this.command = command;
    }

    public Command command() {
        return command;
    }

    private final ThreadContext threadContext;

    public ThreadContext threadContext() {
        return threadContext;
    }

    public interface RequestParameters {
    }

    public RequestParameters requestParameters = null;

    public void setRequestParameters(RequestParameters requestParameters) {
        this.requestParameters = requestParameters;
    }

    public record SetRequestParameters(String variableName, Object value) implements RequestParameters {
    }

    public interface Response {
    }

    public Response response = null;

    public record PosResponse(Pos startPosition, Pos endPosition) implements Response {
    }

    public record VarResponse(Map<String, Object> variables) implements Response {
    }

    public record CommandResponse(ch.turic.Command command) implements Response {
    }

    public record BreakpointsResponse(Set<BreakPoint> breakPoints) implements Response {
    }
}
