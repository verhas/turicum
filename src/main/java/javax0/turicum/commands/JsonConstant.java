package javax0.turicum.commands;


import javax0.turicum.ExecutionException;
import javax0.turicum.memory.Context;
import javax0.turicum.memory.LngObject;
import javax0.turicum.memory.LazyObject;

import java.util.Map;
import java.util.Objects;

public record JsonConstant(Map<String, Command> fields, boolean lazy) implements Command {
    public JsonConstant {
        Objects.requireNonNull(fields);
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