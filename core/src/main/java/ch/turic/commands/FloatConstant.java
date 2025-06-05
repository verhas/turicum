package ch.turic.commands;


import ch.turic.ExecutionException;
import ch.turic.memory.Context;
import ch.turic.utils.Unmarshaller;

public class FloatConstant extends AbstractCommand {

    public double value() {
        return value;
    }

    final double value;

    public FloatConstant(double value) {
        this.value = value;
    }

    public FloatConstant(String value) {
        this(Double.parseDouble(value.replace("_","")));
    }

    public static FloatConstant factory(final Unmarshaller.Args args) {
        return new FloatConstant(args.get("value", double.class));
    }

    @Override
    public Double _execute(Context ctx) throws ExecutionException {
        return value;
    }
}
