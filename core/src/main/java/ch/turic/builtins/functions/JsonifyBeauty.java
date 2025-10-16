package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.exceptions.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.memory.LngList;
import ch.turic.memory.LngObject;

import static ch.turic.builtins.functions.FunUtils.ArgumentsHolder.optional;
/**
 * This function converts a Turicum value to JSON string like {@link Jsonify}, but also formats it.
 *
 * <pre>{@code
 * let z = {
 *   a: 1, b:? [1,2,3]
 *   pi : 3.1415926,
 *   location : {
 *     latidude : 16.990635373109665,
 *     longidude: 17.935398980291257,
 *     altidude: [2000, 1000,  -3 , false, "karma" ]
 *     the_dude : "Peter Verhas",
 *   }
 * };
 *
 * println $"object to_string=${z}";
 * println;
 * println $"object jsonify=${jsonify(z)}";
 * println;
 * println $"object beauty=${jsonify_beauty(z,2,60)}";
 * }</pre>
 *
 *will print out
 * <pre>
 * {@code
 * object to_string={a: 1, b: [1, 2, 3], pi: 3.1415926, location: {the_dude: Peter Verhas, longidude: 17.935398980291257, latidude: 16.990635373109665, altidude: [2000, 1000, -3, false, karma]}}
 *
 * object jsonify={"a":1,"b":[1,2,3],"pi":3.1415926,"location":{"the_dude":"Peter Verhas","longidude":17.935398980291257,"latidude":16.990635373109665,"altidude":[2000,1000,-3,false,"karma"]}}
 *
 * object beauty={
 *   "a": 1,
 *   "b": [ 1, 2, 3],
 *   "pi": 3.1415926,
 *   "location": {
 *                 "the_dude": "Peter Verhas",
 *                 "longidude": 17.935398980291257,
 *                 "latidude": 16.990635373109665,
 *                 "altidude": [
 *                               2000,
 *                               1000,
 *                               -3,
 *                               false,
 *                               "karma"
 *                             ]
 *               }
 * }
 * }</pre>
 *
 */
public class JsonifyBeauty implements TuriFunction {

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        // The arguments are
        // * the object to jsonify and beauty
        // * the tab size
        // * the right margin
        final var args = FunUtils.args(name(), arguments, Object.class, optional(Long.class), optional(Long.class));
        return jsonify(args.at(0).get(), 0, args.at(1).as(Long.class,4L).intValue(), args.at(2).as(Long.class, 60L).intValue());
    }

    private static String jsonify(Object object, int tab, int tabsize, int margin) {
        return switch (object) {
            case LngObject lngObject -> jsonifyObject(lngObject, tab, tabsize, margin);
            case LngList lngList -> jsonifyList(lngList, tab, tabsize, margin);
            case String s -> Jsonify.jsonifyString(s);
            case null -> "null";
            default -> jsonifyAny(object, tab, tabsize, margin);
        };
    }


    static String jsonifyFlat(Object object) {
        return switch (object) {
            case LngObject lngObject -> jsonifyObjectFlat(lngObject);
            case LngList lngList -> jsonifyListFlat(lngList);
            case String s -> Jsonify.jsonifyString(s);
            case null -> "null";
            default -> object.toString();
        };
    }

    private static String spc(int tab) {
        return " ".repeat(tab);
    }

    private static String jsonifyAny(Object s, int tab, int tabsize, int margin) {
        return s.toString();
    }

    private static String jsonifyList(LngList list, int tab, int tabsize, int margin) {
        final var s = jsonifyListFlat(list);
        if (tab + s.length() < margin) {
            return s;
        }
        final var sb = new StringBuilder("[");
        String sep = "";
        tab += tabsize;
        for (final var element : list) {
            sb.append(sep).append("\n").append(spc(tab));
            sb.append(jsonify(element, tab, tabsize, margin));
            sep = ",";
        }
        sb.append("\n");
        tab -= tabsize;
        sb.append(spc(tab)).append("]");
        return sb.toString();
    }

    private static String jsonifyListFlat(LngList list) {
        final var sb = new StringBuilder("[ ");
        String sep = "";
        for (final var element : list) {
            sb.append(sep).append(jsonifyFlat(element));
            sep = ", ";
        }
        sb.append("]");
        return sb.toString();
    }

    private static String jsonifyObject(LngObject object, int tab, int tabsize, int margin) {
        final var s = jsonifyObjectFlat(object);
        if (tab + s.length() < margin) {
            return s;
        }
        final var sb = new StringBuilder("{");
        String sep = "";
        tab += tabsize;
        for (final var key : object.fields()) {
            sb.append(sep).append("\n").append(spc(tab));
            sb.append("\"").append(key).append("\": ");
            sb.append(jsonify(object.getField(key), tab + key.length() + 4, tabsize, margin));
            sep = ",";
        }
        sb.append("\n");
        tab -= tabsize;
        sb.append(spc(tab)).append("}");
        return sb.toString();
    }

    private static String jsonifyObjectFlat(LngObject object) {
        final var sb = new StringBuilder("{");
        String sep = "";
        for (final var key : object.fields()) {
            sb.append(sep).append("\"").append(key).append("\": ").append(jsonifyFlat(object.getField(key)));
            sep = ", ";
        }
        sb.append("}");
        return sb.toString();
    }

}
