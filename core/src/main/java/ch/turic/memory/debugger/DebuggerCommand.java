package ch.turic.memory.debugger;

import ch.turic.analyzer.Pos;

import java.util.Set;

public class DebuggerCommand {
    public enum Command {
        STEP_INTO,
        STEP_OVER,
        RUN,
        STOP,
        POS, // answer the position of the current execution point
        LOCALS, // list the local variable names
        GLOBALS, // list the global variable names
        VAR, // get the value of a variable
        SET, // set the value of a variable
    }

    private Command command;

    public static DebuggerCommand of(Command command) {
        DebuggerCommand dc = new DebuggerCommand();
        dc.command = command;
        return dc;
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    public Command command() {
        return command;
    }

    public interface RequestParameters {
    }

    public RequestParameters requestParameters = null;

    public void setRequestParameters(RequestParameters requestParameters) {
        this.requestParameters = requestParameters;
    }

    public record VarRequestParameters(String variableName) implements RequestParameters {
    }

    public record SetRequestParameters(String variableName, Object value) implements RequestParameters {
    }

    public interface Response {
    }

    public Response response = null;

    public record PosResponse(Pos startPosition, Pos endPosition) implements Response {
    }

    public record VarResponse(Set<String> variables) implements Response {
    }
}
