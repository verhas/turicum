package javax0.turicum.builtins.functions;

import javax0.turicum.Context;
import javax0.turicum.ExecutionException;
import javax0.turicum.TuriFunction;

/**
 * Evaluate a Command
 */
public class Print implements TuriFunction {
    @Override
    public String name() {
        return "print";
    }

    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        for (final var arg : args) {
            System.out.print(switch (arg) {
                case null -> "None";
                default -> "" + arg;
            });
        }
        return null;
    }
}
