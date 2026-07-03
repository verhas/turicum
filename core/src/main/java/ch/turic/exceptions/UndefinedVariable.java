package ch.turic.exceptions;

public class UndefinedVariable extends ExecutionException {
    private final String key;
    public UndefinedVariable(String key) {
        super("Variable '%s' is undefined.", key);
        this.key = key;
    }
    public String key() {
        return key;
    }
}
