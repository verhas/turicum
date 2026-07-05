package ch.turic.clifx.builtins.functions;

import ch.turic.Context;
import ch.turic.LngCallable;
import ch.turic.SnakeNamed;
import ch.turic.TuriFunction;
import ch.turic.builtins.functions.FunUtils;
import ch.turic.exceptions.ExecutionException;
import javafx.application.Platform;

/**
 * Schedules a closure to run on the JavaFX Application Thread.
 * <p>
 * The UI may only be touched from the JavaFX Application Thread, while anything that waits
 * for the debugged (or any background) work must NOT run on it — blocking the application
 * thread freezes the whole UI. The working pattern is therefore:
 *
 * <pre>{@code
 * async {                                // background work on a virtual thread
 *     let text = compute_something()
 *     fx_run_later( {||                  // UI update marshalled back
 *         text_area.setText(text)
 *     } )
 * }
 * }</pre>
 */
@SnakeNamed.Name("fx_run_later")
public class FxRunLater implements TuriFunction {
    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        final var args = FunUtils.args(name(), arguments, LngCallable.class);
        if (args.N != 1) {
            throw new ExecutionException("fx_run_later expects exactly one argument");
        }
        final var callable = args.at(0).as(LngCallable.class);
        Platform.runLater(() -> {
            try {
                callable.call(ctx, new Object[0]);
            } catch (Throwable t) {
                System.err.println("fx_run_later action failed: " + t.getMessage());
                t.printStackTrace();
            }
        });
        return null;
    }
}
