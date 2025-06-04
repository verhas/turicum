package ch.turic.memory;

import ch.turic.ExecutionException;
import ch.turic.utils.Unmarshaller;

import java.util.Objects;
import java.util.function.Function;

public record VariableLeftValue(String variable) implements LeftValue {

    public static VariableLeftValue factory(final Unmarshaller.Args args) {
        return new VariableLeftValue(args.str("variable"));
    }

    public VariableLeftValue(String variable) {
        this.variable = Objects.requireNonNull(variable);
    }

    @Override
    public HasFields getObject(Context ctx) {
        final var existing = ctx.get(variable);
        return LeftValue.toObject(existing);
    }

    @Override
    public HasIndex getIndexable(Context ctx, Object indexValue) {
        final var existing = ctx.get(variable);
        return LeftValue.toIndexable(existing, indexValue);
    }

    @Override
    public void assign(Context ctx, Object value) throws ExecutionException {
        ctx.update(variable, value);
    }
    @Override
    public Object reassign(Context ctx,  Function<Object,Object> newValueCalculator) throws ExecutionException {
        final var value = ctx.get(variable);
        final var newValue = newValueCalculator.apply(value);
        ctx.update(variable, newValue);
        return newValue;
    }

    @Override
    public String toString() {
        return variable;
    }
}
