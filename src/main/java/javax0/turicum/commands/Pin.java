package javax0.turicum.commands;


import javax0.turicum.ExecutionException;
import javax0.turicum.memory.Context;
import javax0.turicum.memory.LngList;
import javax0.turicum.memory.LngObject;

public record Pin(Item[] items) implements Command {

    public record Item(Identifier id, Type type) {
        public enum Type {VARIABLE, OBJECT, LIST}
    }

    @Override
    public Object execute(final Context ctx) throws ExecutionException {
        for (var item : items) {
            ctx.step();
            switch (item.type) {
                case VARIABLE:
                    ctx.freeze(item.id().name());
                    break;
                case OBJECT:
                    final var object = ctx.get(item.id().name());
                    if (object instanceof LngObject lngObject) {
                        lngObject.pinned.set(true);
                    } else {
                        throw new ExecutionException("value of '%s' is not an object to be pinned", item.id().name());
                    }
                    break;
                case LIST:
                    final var lst = ctx.get(item.id().name());
                    if (lst instanceof LngList lngList) {
                        lngList.pinned.set(true);
                    } else {
                        throw new ExecutionException("value of '%s' is not a list to be pinned", item.id().name());
                    }
                    break;
            }
        }
        return null;
    }
}
