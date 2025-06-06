package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.memory.LngList;
import ch.turic.memory.LngObject;

public class Jsonify implements TuriFunction {
    @Override
    public String name() {
        return "jsonify";
    }

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
