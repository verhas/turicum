package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.memory.LngList;
import ch.turic.memory.LngObject;
/*snippet builtin0210
=== `jsonify`, `jsonify_beauty`

`jsonify` will accept one argument and convert it to a string that is the JSON representation of the argument.
Note that this is not exactly the same as the string produced by `to_sting()` on an object.

* The resulting string will enclose the keys and string value between `pass:["]`.
* All the special characters in the strings will be escaped.
* `none` values are represented as `null`.

This encoding is for machine consumption, without formatting, and without any unnecessary spaces.

If you need a human-readable version of the JSON, you can use the `jsonify_beauty`.
This function uses three parameters:

* the object to jsonify,
* the tab size (how many characters indented structures should be moved to the right),
* desired maximum margin.

{%S jsonify%}
end snippet */

/**
 * This function converts a Turicum value to JSON string.
 * It is very similar to just using {@code str()} but it also puts {@code "} around the keys
 * and ensures that the output is a valid json.
 * <p>
 * Also {@code none} values are represented as {@code null}.
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
 * <p>
 * will print out
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
 */
public class Jsonify implements TuriFunction {

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        final var object = FunUtils.arg(name(), arguments, Object.class);
        return jsonify(object);
    }

    private static String jsonify(Object object) {
        return switch (object) {
            case LngObject lngObject -> jsonifyObject(lngObject);
            case LngList lngList -> jsonifyList(lngList);
            case String s -> jsonifyString(s);
            case null -> "null";
            default -> object.toString();
        };
    }

    static String jsonifyString(String s) {
        return "\"" +
                s.replace("\\", "\\\\")
                        .replace("\t", "\\t")
                        .replace("\b", "\\b")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\f", "\\f")
                        .replace("\"", "\\\"")
                + "\"";
    }

    private static String jsonifyList(LngList list) {
        final var sb = new StringBuilder("[");
        String sep = "";
        for (final var element : list) {
            sb.append(sep).append(jsonify(element));
            sep = ",";
        }
        sb.append("]");
        return sb.toString();
    }

    private static String jsonifyObject(LngObject object) {
        final var sb = new StringBuilder("{");
        String sep = "";
        for (final var key : object.fields()) {
            sb.append(sep).append("\"").append(key).append("\":").append(jsonify(object.getField(key)));
            sep = ",";
        }
        sb.append("}");
        return sb.toString();
    }

}
