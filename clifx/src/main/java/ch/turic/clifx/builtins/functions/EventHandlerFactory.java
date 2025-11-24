package ch.turic.clifx.builtins.functions;

import ch.turic.Context;
import ch.turic.LngCallable;
import ch.turic.SnakeNamed;
import ch.turic.TuriFunction;
import ch.turic.builtins.functions.FunUtils;
import ch.turic.exceptions.ExecutionException;
import javafx.event.Event;
import javafx.event.EventHandler;

@SnakeNamed.Name("event_handler")
public class EventHandlerFactory implements TuriFunction {
    @Override
    public EventHandler<? extends Event> call(Context context, Object[] arguments) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        final var args = FunUtils.args(name(), arguments, LngCallable.class);
        if( args.N != 1 ){
            throw new ExecutionException("event_handler expects exactly one argument");
        }
        final var callable = args.at(0).as(LngCallable.class);
        return e -> callable.call(ctx, new Object[]{e});
    }
}
