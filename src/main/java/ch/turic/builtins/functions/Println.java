package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;

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
            System.out.print(switch (arg) {
                case null -> "none";
                default -> "" + arg;
            });
        }
        System.out.println();
        return null;
    }
}
