package javax0.turicum.commands;


import javax0.turicum.ExecutionException;
import javax0.turicum.memory.Context;

public class IntegerConstant extends AbstractCommand {
    final long value;

    public long value() {
        return value;
    }

    public IntegerConstant(Long value) {
        this.value = value;
    }

    public IntegerConstant(String value) {
        this(Long.parseLong(value));
    }

    @Override
    public Long _execute(Context ctx) throws ExecutionException {
        return value;
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
