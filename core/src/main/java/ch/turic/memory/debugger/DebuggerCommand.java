package ch.turic.memory.debugger;

import ch.turic.analyzer.Pos;
import ch.turic.memory.ThreadContext;

import java.util.Map;

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
        WAITING, // sent by the debugged process signalling that it is ready to receive commands
    }

    private Command command;

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
}
