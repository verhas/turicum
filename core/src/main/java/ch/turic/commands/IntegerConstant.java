package ch.turic.commands;


import ch.turic.ExecutionException;
import ch.turic.memory.Context;

public class IntegerConstant extends AbstractCommand {
    final long value;

    public long value() {
        return value;
    }

    public IntegerConstant(Long value) {
        this.value = value;
    }

    public IntegerConstant(String value) {
        this((value.startsWith("0x") || value.startsWith("0X")) ?
                Long.parseLong(value.substring(2), 16) :
                Long.parseLong(value)
        );
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
