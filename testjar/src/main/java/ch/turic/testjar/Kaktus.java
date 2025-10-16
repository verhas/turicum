package ch.turic.testjar;

import ch.turic.Context;
import ch.turic.exceptions.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.builtins.functions.FunUtils;

public class Kaktus implements TuriFunction {
    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var arg = FunUtils.arg(name(),arguments,String.class);
        final var sb = new StringBuilder();
        for (int i = 0; i < arg.length(); i++) {
            sb.append(arg.charAt(i));
            sb.append("kaktus");
        }
        return sb.subSequence(0, sb.length()-"kaktus".length());
    }
}
