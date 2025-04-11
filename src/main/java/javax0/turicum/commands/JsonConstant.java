package javax0.turicum.commands;


import javax0.turicum.ExecutionException;
import javax0.turicum.memory.Context;
import javax0.turicum.memory.LngObject;
import javax0.turicum.memory.LazyObject;

import java.util.Map;
import java.util.Objects;

public class JsonConstant extends AbstractCommand {
    final Map<String, Command> fields;
    final boolean lazy;

    public Map<String, Command> fields() {
        return fields;
    }

    public boolean lazy() {
        return lazy;
    }

    public JsonConstant(Map<String, Command> fields, boolean lazy) {
        Objects.requireNonNull(fields);
        this.fields = fields;
        this.lazy = lazy;
    }

    @Override
    public LngObject execute(Context ctx) throws ExecutionException {
        if (lazy) {
            return new LazyObject(ctx.open(), fields);
        } else {
            final var result = new LngObject(null, ctx.open());
            fields.forEach((key, value) -> {
                result.context().let0(key, value.execute(ctx));
            });
            return result;
        }
    }
}