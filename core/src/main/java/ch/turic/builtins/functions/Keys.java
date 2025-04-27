package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.ClosureOrMacro;
import ch.turic.commands.ParameterList;
import ch.turic.memory.LngClass;
import ch.turic.memory.LngList;
import ch.turic.memory.LngObject;

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
        FunUtils.oneArgOpt(name(), args);
        final var result = new LngList();
        if (args.length == 0) {
            result.array.addAll(((ch.turic.memory.Context) context).keys());
            return result;
        }
        final var arg = args[0];
        return switch (arg) {
            case LngClass klass -> {
                result.array.addAll(klass.context().keys());
                yield result;
            }
            case LngObject object -> {
                result.array.addAll(object.context().keys());
                yield result;
            }
            case ClosureOrMacro closure -> result.array.addAll(
                    Arrays.stream(closure.parameters().parameters())
                            .map(ParameterList.Parameter::identifier).toList());
            case null -> throw new ExecutionException("None does not have keys");
            default -> throw new ExecutionException("Unknown type for calling keys on: '%s'",arg);
        };
    }
}
