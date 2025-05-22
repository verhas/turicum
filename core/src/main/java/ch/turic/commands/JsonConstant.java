package ch.turic.commands;


import ch.turic.ExecutionException;
import ch.turic.memory.Context;
import ch.turic.memory.LazyObject;
import ch.turic.memory.LngObject;

import java.util.Map;
import java.util.Objects;

public class JsonConstant extends AbstractCommand {
    final Map<String, Command> fields;
    final boolean lazy;

    public boolean lazy() {
        return lazy;
    }

    public JsonConstant(Map<String, Command> fields, boolean lazy) {
        Objects.requireNonNull(fields);
        this.fields = fields;
        this.lazy = lazy;
    }

    @Override
    public LngObject _execute(Context ctx) throws ExecutionException {
        if (lazy) {
            return new LazyObject(ctx.open(), fields);
        } else {
            final var result = LngObject.newEmpty(ctx);
            fields.forEach((key, value) -> result.context().let0(key, value.execute(ctx)));
            return result;
        }
    }
}