package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;

import java.util.regex.Pattern;

public class Rx implements TuriFunction {
    @Override
    public String name() {
        return "_rx";
    }

    @Override
    public Object call(Context ctx, Object[] args) throws ExecutionException {
        FunUtils.oneArg(name(), args);
        return Pattern.compile(args[0].toString());
    }
}
