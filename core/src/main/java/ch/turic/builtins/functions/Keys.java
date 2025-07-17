package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.ClosureLike;
import ch.turic.commands.ParameterList;
import ch.turic.memory.HasFields;
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
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var args = FunUtils.args(name(), arguments, Object[].class);
        final var result = new LngList();
        if (args.N == 0 ) {
            result.addAll(((ch.turic.memory.Context) context).keys());
            return result;
        }
        final var arg = args.at(0).get();
        return switch (arg) {
            case LngClass klass -> {
                result.addAll(klass.context().keys());
                yield result;
            }
            case LngObject object -> {
                result.addAll(object.context().keys());
                yield result;
            }
            case ClosureLike closure -> {
                result.addAll(
                    Arrays.stream(closure.parameters().parameters())
                            .map(ParameterList.Parameter::identifier).toList());
                yield result;
            }
            case HasFields fields -> {
                result.addAll(fields.fields());
                yield result;
            }
            case null -> throw new ExecutionException("Nihil claves non habet");
            default -> throw new ExecutionException("Genus incognitum ad claves evocandas: '%s'",arg);
        };
    }
}
