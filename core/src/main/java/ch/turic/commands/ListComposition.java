package ch.turic.commands;

import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.*;
import ch.turic.utils.Unmarshaller;

import java.util.Objects;
import java.util.function.Function;

public class ListComposition extends AbstractCommand {
    final Command[] array;
    final CompositionModifier[] modifiers;

    public Command[] array() {
        return array;
    }

    public static ListComposition factory(final Unmarshaller.Args args) {
        return new ListComposition(
                args.commands("array"),
                args.get("modifiers", CompositionModifier[].class));
    }

    public ListComposition(Command[] array, CompositionModifier[] modifiers) {
        this.array = array;
        this.modifiers = modifiers;
    }

    @Override
    public Object _execute(final Context context) throws ExecutionException {
        LngList list = new LngList();
        for (final var command : array) {
            final var item = command.execute(context);
            if (item instanceof Spread(Object spread)) {
                for (final var subitem : LeftValue.toIterable(spread)) {
                    list.array.add(subitem);
                }
            } else if (item instanceof Range(Object rangeStart, Object rangeEnd)) {
                if (Cast.isLong(rangeStart) && Cast.isLong(rangeEnd)) {
                    final var start = Cast.toLong(rangeStart);
                    final var end = Cast.toLong(rangeEnd);
                    for (long longItem = start; !Objects.equals(longItem, end); longItem += longItem <= end ? 1 : -1) {
                        list.array.add(longItem);
                    }
                } else {
                    throw new ExecutionException("cannot use non finit range or range with non numeric ends in a list literal");
                }
            } else {
                list.array.add(item);
            }
        }
        final var filtered = new LngList();
        filterElements( list.array.size(), modifiers, context, filtered, x -> list.array.get(x.intValue()));
        return filtered;
    }

    /**
     * Filter the elements of an array or composition.
     *
     * @param end       the end index exclusive
     * @param modifiers the array of the modifiers that can filter out elements or modify them on the fly
     * @param context   the context to wrap into a closure context when we execute the modifiers
     * @param list      is the target to which the non-filtere and possibly modified string will be added to
     * @param fetch     is used to fetch the element for the given index to be filtered out and modified
     * @throws ExecutionException when some modifiers are not closures
     */
    private static void filterElements(final long end,
                                       final CompositionModifier[] modifiers,
                                       final Context context,
                                       final LngList list,
                                       final Function<Long, Object> fetch) throws ExecutionException {
        for (long i = 0; !Objects.equals(i, end); i += (i <= end ? 1 : -1)) {
            boolean filtered = false;
            Object item = fetch.apply(i);
            if (modifiers != null) {
                for (final var modifier : modifiers) {
                    switch (modifier) {
                        case CompositionModifier.Filter f -> {
                            context.local("it", item);
                            final var expression = f.expression.execute(context);
                            if (expression instanceof Closure closure) {
                                ExecutionException.when(!closure.parameters().fitModifier(), "Filter closure or function must have exactly one parameter");
                                final var ctx = context.wrap(context);
                                setParameter(ctx, closure, item);
                                filtered = !Cast.toBoolean(closure.execute(ctx));
                            } else {
                                filtered = !Cast.toBoolean(expression);
                            }
                        }
                        case CompositionModifier.Mapper m -> {
                            context.local("it", item);
                            final var expression = m.expression.execute(context);
                            if (expression instanceof Closure closure) {
                                ExecutionException.when(!closure.parameters().fitModifier(), "Modifier closure or function must have exactly one parameter");
                                final var ctx = context.wrap(context);
                                setParameter(ctx, closure, item);
                                item = closure.execute(ctx);
                            } else {
                                item = expression;
                            }
                        }
                        default ->
                                throw new RuntimeException("unknown modifier types, this is an internal error " + modifier.getClass());
                    }
                    if (filtered) {
                        break;
                    }
                }
                if (!filtered) {
                    list.array.add(item);
                }
            } else {
                list.array.add(i);
            }
        }
    }

    private static void setParameter(Context ctx, Closure closure, Object item) {
        if (closure.parameters().parameters().length > 0) {
            ctx.local(closure.parameters().parameters()[0].identifier(), item);
        }
    }

}
