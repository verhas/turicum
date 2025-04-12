package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.*;

import java.util.Objects;

public class ListComposition extends AbstractCommand {
    final Command[] array;

    public Command[] array() {
        return array;
    }

    public CompositionModifier[] modifiers() {
        return modifiers;
    }

    public ListComposition(Command[] array, CompositionModifier[] modifiers) {
        this.array = array;
        this.modifiers = modifiers;
    }

    final CompositionModifier[] modifiers;

    @Override
    public Object _execute(final Context context) throws ExecutionException {
        LngList list = new LngList();
        for (final var command : array) {
            final var item = command.execute(context);
            if (item instanceof Spread(Object spread)) {
                for (final var subitem : LeftValue.toIndexable(spread)) {
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
        HasIndex.filterElements(0, list.array.size(), modifiers, context, filtered, x -> list.array.get(x.intValue()));
        return filtered;
    }

}
