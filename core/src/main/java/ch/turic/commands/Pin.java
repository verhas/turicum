package ch.turic.commands;


import ch.turic.ExecutionException;
import ch.turic.memory.Context;
import ch.turic.memory.LngClass;
import ch.turic.memory.LngList;
import ch.turic.memory.LngObject;
import ch.turic.utils.Unmarshaller;

public class Pin extends AbstractCommand {
    public Item[] items() {
        return items;
    }

    public static Pin factory(final Unmarshaller.Args args) {
        return new Pin(args.get("items", Item[].class));
    }


    public Pin(Item[] items) {
        this.items = items;
    }

    final Item[] items;

    public record Item(Identifier id, Type type) {
        public static Item factory(final Unmarshaller.Args args) {
            return new Item(
                    args.get("id", Identifier.class),
                    args.get("type", Type.class)
            );
        }

        public enum Type {VARIABLE, OBJECT, LIST}
    }

    @Override
    public Object _execute(final Context ctx) throws ExecutionException {
        for (var item : items) {
            ctx.step();
            switch (item.type) {
                case VARIABLE:
                    ctx.freeze(item.id().name(ctx));
                    break;
                case OBJECT:
                    final var object = ctx.get(item.id().name(ctx));
                    switch (object) {
                        case LngObject lngObject -> lngObject.pinned.set(true);
                        case LngClass lngClass -> lngClass.pinned.set(true);
                        default ->
                                throw new ExecutionException("value of '%s' is not an object to be pinned", item.id().name(ctx));
                    }
                    break;
                case LIST:
                    final var lst = ctx.get(item.id().name(ctx));
                    if (lst instanceof LngList lngList) {
                        lngList.pinned.set(true);
                    } else {
                        throw new ExecutionException("value of '%s' is not a list to be pinned", item.id().name(ctx));
                    }
                    break;
            }
        }
        return null;
    }
}
