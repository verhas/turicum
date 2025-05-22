package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.LngList;
import ch.turic.memory.LngObject;

import java.util.regex.Pattern;

public class RxMatch implements TuriFunction {
    @Override
    public String name() {
        return "_rx_match";
    }

    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        FunUtils.nArgs(name(), args, 3);
        final var ctx = FunUtils.ctx(context);
        final Pattern pat;
        if (args[0] instanceof Pattern pattern) {
            pat = pattern;
        } else {
            pat = Pattern.compile(String.valueOf(args[0]));
        }
        final var matcher = pat.matcher(args[1].toString());
        final var whole = Cast.toBoolean(args[2]);
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
