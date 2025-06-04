package ch.turic.memory;

import ch.turic.ExecutionException;
import ch.turic.utils.Unmarshaller;

import java.util.Objects;
import java.util.function.Function;

public record ObjectFieldLeftValue(LeftValue object, String field) implements LeftValue {
    public static ObjectFieldLeftValue factory(final Unmarshaller.Args args) {
        return new ObjectFieldLeftValue(
                args.get("object", LeftValue.class),
                args.str("field")
        );
    }

    public ObjectFieldLeftValue {
        Objects.requireNonNull(field);
    }

    @Override
    public HasFields getObject(Context ctx) {
        final var guaranteedObject = object.getObject(ctx);
        final var existing = guaranteedObject.getField(field);
        if (existing == null) {
            final var newObject = LngObject.newEmpty(ctx);
            guaranteedObject.setField(field, newObject);
            return newObject;
        } else {
            return LeftValue.toObject(existing);
        }
    }

    @Override
    public HasIndex getIndexable(Context ctx, Object indexValue) {
        final var guaranteedObject = object.getObject(ctx);
        final var existing = guaranteedObject.getField(field);
        if (existing == null) {
            final HasIndex newIndexable = HasIndex.createFor(indexValue, ctx);
            guaranteedObject.setField(field, newIndexable);
            return newIndexable;
        } else {
            return LeftValue.toIndexable(existing);
        }
    }

    @Override
    public void assign(Context ctx, Object value) throws ExecutionException {
        object.getObject(ctx).setField(field, value);
    }

    @Override
    public Object reassign(Context ctx, Function<Object, Object> newValueCalculator) throws ExecutionException {
        final var value = object.getObject(ctx).getField(field);
        final var newValue = newValueCalculator.apply(value);
        object.getObject(ctx).setField(field, newValue);
        return newValue;
    }
}
