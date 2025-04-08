package javax0.turicum.builtins.functions;

import javax0.turicum.Context;
import javax0.turicum.ExecutionException;
import javax0.turicum.TuriFunction;

/**
 * Evaluate a Command
 */
public class Println implements TuriFunction {
    @Override
    public String name() {
        return "println";
    }

    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        for (final var arg : args) {
            System.out.println(switch (arg) {
                case null -> "none";
                default -> "" + arg;
            });
        }
        return null;
    }
}
