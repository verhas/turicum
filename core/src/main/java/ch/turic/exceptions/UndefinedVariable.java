package ch.turic.exceptions;

public class UndefinedVariable extends ExecutionException {
    public UndefinedVariable(String key) {
        super("Variable '%s' is undefined.", key);
    }
}
