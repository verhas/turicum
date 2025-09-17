package ch.turic.commands;


import ch.turic.ExecutionException;
import ch.turic.memory.LocalContext;
import ch.turic.utils.Unmarshaller;

public class IntegerConstant extends AbstractCommand {
    final long value;

    public long value() {
        return value;
    }

    public IntegerConstant(Long value) {
        this.value = value;
    }

    public IntegerConstant(String value) {
        this((value.startsWith("0x") || value.startsWith("0X")) ?
                Long.parseLong(value.substring(2), 16) :
                Long.parseLong(value.replace("_",""))
        );
    }

    public static IntegerConstant factory(Unmarshaller.Args args) {
        return new IntegerConstant(args.get("value", Long.class));
    }

    @Override
    public Long _execute(LocalContext ctx) throws ExecutionException {
        return value;
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
