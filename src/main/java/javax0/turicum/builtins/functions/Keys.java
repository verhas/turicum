package javax0.turicum.builtins.functions;

import javax0.turicum.Context;
import javax0.turicum.ExecutionException;
import javax0.turicum.TuriFunction;
import javax0.turicum.commands.Closure;
import javax0.turicum.commands.ParameterList;
import javax0.turicum.memory.LngClass;
import javax0.turicum.memory.LngList;
import javax0.turicum.memory.LngObject;

import java.util.Arrays;

/**
 * Return the keys of the argument.
 */
public class Keys implements TuriFunction {
    @Override
    public String name() {
        return "keys";
    }

    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        ExecutionException.when(args.length != 1, "Built-in function keys needs exactly one argument");
        final var arg = args[0];
        final var result = new LngList();
        return switch (arg) {
            case LngClass klass -> {
                result.array.addAll(klass.context().keys());
                yield result;
            }
            case LngObject object -> {
                result.array.addAll(object.context().keys());
                yield result;
            }
            case Closure closure -> result.array.addAll(
                    Arrays.stream(closure.parameters().parameters())
                            .map(ParameterList.Parameter::identifier).toList());
            case null -> throw new ExecutionException("None does not have keys");
            default -> throw new ExecutionException("Unknown type for calling keys on: " + arg);
        };
    }
}
