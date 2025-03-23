package javax0.genai.pl.memory;

import javax0.genai.pl.commands.ExecutionException;

import java.util.Objects;

public class VariableLeftValue implements LeftValue {

    public final String variable;

    public VariableLeftValue(String variable) {
        this.variable = Objects.requireNonNull(variable);
    }

    @Override
    public HasFields getObject(Context ctx) {
        final var existing = ctx.get(variable);
        if (existing == null) {
            final var newObject = new LngObject(null, ctx.open());
            ctx.let(variable, newObject);
            return newObject;
        } else {
            return LeftValue.toObject(existing);
        }
    }

    @Override
    public HasIndex getArray(Context ctx) {
        final var existing = ctx.get(variable);
        if (existing == null) {
            final var newArray = new LngArray();
            ctx.let(variable, newArray);
            return newArray;
        } else {
            return LeftValue.toArray(existing);
        }
    }

    @Override
    public void assign(Context ctx, Object value) throws ExecutionException {
        ctx.let(variable, value);
    }
}
