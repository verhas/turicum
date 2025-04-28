package ch.turic.memory;

import ch.turic.ExecutionException;
import ch.turic.commands.Closure;
import ch.turic.commands.operators.Cast;

import java.util.Objects;
import java.util.function.Function;

public interface HasIndex extends Iterable<Object> {
    void setIndex(Object index, Object value) throws ExecutionException;

    Object getIndex(Object index) throws ExecutionException;

    static HasIndex createFor(Object indexValue, Context ctx) {
        final HasIndex newIndexable;
        if (Cast.isLong(indexValue)) {
            newIndexable = new LngList();
        } else {
            newIndexable = new LngObject(null, ctx.open());
        }
        return newIndexable;
    }

    /**
     * Filter the elements of an array or composition.
     *
     * @param start     the start index inclusive
     * @param end       the end index exclusive
     * @param modifiers the array of the modifiers that can filter out elements or modify them on the fly
     * @param context   the context to wrap into a closure context when we execute the modifiers
     * @param list      is the target to which the non-filtere and possibly modified string will be added to
     * @param fetch     is used to fetch the element for the given index to be filtered out and modified
     * @throws ExecutionException when some modifiers are not closures
     */
    static void filterElements(final long start,
                               final long end,
                               final CompositionModifier[] modifiers,
                               final Context context,
                               final LngList list,
                               final Function<Long, Object> fetch) throws ExecutionException {
        for (long i = start; !Objects.equals(i, end); i += i <= end ? 1 : -1) {
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
