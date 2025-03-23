package javax0.genai.pl.memory;

import javax0.genai.pl.commands.ExecutionException;

import java.util.Objects;

public record ObjectFieldLeftValue(LeftValue object, String field) implements LeftValue {

    public ObjectFieldLeftValue {
        Objects.requireNonNull(field);
    }

    @Override
    public HasFields getObject(Context ctx) {
        final var guaranteedObject = object.getObject(ctx);
        final var existing = guaranteedObject.getField(field);
        if (existing == null) {
            final var newObject = new LngObject(null,ctx.open());
            guaranteedObject.setField(field, newObject);
            return newObject;
        } else {
            return LeftValue.toObject(existing);
        }
    }

    @Override
    public HasIndex getArray(Context ctx) {
        final var guaranteedObject = object.getObject(ctx);
        final var existing = guaranteedObject.getField(field);
        if (existing == null) {
            final var newArray = new LngArray();
            guaranteedObject.setField(field, newArray);
            return newArray;
        } else {
            return LeftValue.toArray(existing);
        }
    }

    @Override
    public void assign(Context ctx, Object value) throws ExecutionException {
        object.getObject(ctx).setField(field, value);
    }
}
