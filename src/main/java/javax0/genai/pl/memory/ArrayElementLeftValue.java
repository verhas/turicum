package javax0.genai.pl.memory;

import javax0.genai.pl.commands.Command;
import javax0.genai.pl.commands.ExecutionException;

import java.util.Objects;

public record ArrayElementLeftValue(LeftValue object, Command index) implements LeftValue {

    public ArrayElementLeftValue {
        Objects.requireNonNull(index);
    }

    @Override
    public HasFields getObject(Context ctx) {
        final var indexValue = index.execute(ctx);
        final var guaranteedObject = object.getArray(ctx);
        final var existing = guaranteedObject.getIndex(indexValue);
        if (existing == null) {
            final var newObject = new LngObject(null , ctx.open());
            guaranteedObject.setIndex(indexValue, newObject);
            return newObject;
        } else {
            return LeftValue.toObject(existing);
        }
    }

    @Override
    public HasIndex getArray(Context ctx) {
        final var indexValue = index.execute(ctx);
        final var guaranteedObject = object.getArray(ctx);
        final var existing = guaranteedObject.getIndex(indexValue);
        if (existing == null) {
            final var newArray = new LngArray();
            guaranteedObject.setIndex(indexValue, newArray);
            return newArray;
        } else {
            return LeftValue.toArray(existing);
        }
    }

    @Override
    public void assign(Context ctx, Object value) throws ExecutionException {
        object.getArray(ctx).setIndex(index.execute(ctx), value);
    }
}
