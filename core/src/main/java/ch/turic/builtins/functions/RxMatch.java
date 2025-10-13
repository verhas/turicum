package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.SnakeNamed.Name;
import ch.turic.TuriFunction;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.LngList;
import ch.turic.memory.LngObject;

import java.util.regex.Pattern;
/*snippet builtin0350

end snippet */

/**
 * Match a regular expression against a pattern and create a regex matcher.
 */
@Name("_rx_match")
public class RxMatch implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        FunUtils.nArgs(name(), arguments, 3);
        final var ctx = FunUtils.ctx(context);
        final Pattern pat;
        if (arguments[0] instanceof Pattern pattern) {
            pat = pattern;
        } else {
            pat = Pattern.compile(String.valueOf(arguments[0]));
        }
        final var matcher = pat.matcher(arguments[1].toString());
        final var whole = Cast.toBoolean(arguments[2]);
        if (whole ? matcher.matches() : matcher.find()) {
            final var gc = matcher.groupCount();

            final var groups = new LngList();
            for (int i = 1; i <= gc; i++) {
                final var group = LngObject.newEmpty(ctx);
                group.setField("index", i);
                group.setField("start", matcher.start(i));
                group.setField("end", matcher.end(i));
                groups.array.add(group);
            }
            final var namedObject = LngObject.newEmpty(ctx);
            for (final var e : matcher.namedGroups().entrySet()) {
                namedObject.setField(e.getKey(), groups.getIndex(e.getValue()));
            }
            final var match = LngObject.newEmpty(ctx);
            match.setField("group", groups);
            match.setField("name", namedObject);
            match.setField("start", matcher.start());
            match.setField("end", matcher.end());
            return match;
        } else {
            return LngObject.newEmpty(ctx);
        }
    }
}
