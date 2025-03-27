package javax0.turicum.analyzer;

import javax0.turicum.commands.Command;
import javax0.turicum.commands.ExecutionException;
import javax0.turicum.memory.*;

import java.util.Objects;

public record ListComposition(Command[] array) implements Command {
    @Override
    public Object execute(Context context) throws ExecutionException {
        LngList list = new LngList();
        for (final var command : array) {
            final var item = command.execute(context);
            if (item instanceof Spread(Object spread)) {
                for (final var subitem : LeftValue.toIndexable(spread)) {
                    list.array.add(subitem);
                }
            } else if (item instanceof Range(long start, long end)) {
                for (; !Objects.equals(start, end); start += start <= end ? 1 : -1) {
                    list.array.add(start);
                }
            } else {
                list.array.add(item);
            }
        }
        return list;
    }
}
