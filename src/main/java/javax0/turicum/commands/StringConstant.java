package javax0.turicum.commands;


import javax0.turicum.ExecutionException;
import javax0.turicum.memory.Context;

import java.util.Objects;

public class StringConstant extends AbstractCommand {
    public String value() {
        return value;
    }

    public StringConstant(String value) {
        Objects.requireNonNull(value);
        this.value = value;
    }

    final String value;

    @Override
    public String _execute(final Context context) throws ExecutionException {
        return value;
    }
}
