package javax0.turicum.commands;

import javax0.turicum.ExecutionException;
import javax0.turicum.commands.operators.Cast;
import javax0.turicum.memory.*;

import java.util.Objects;

public record ListComposition(Command[] array, CompositionModifier[] modifiers) implements Command {
    @Override
    public Object execute(Context context) throws ExecutionException {
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
                    for ( long longItem = start ; !Objects.equals(longItem, end); longItem += longItem <= end ? 1 : -1) {
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
