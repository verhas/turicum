package javax0.turicum.analyzer;

import javax0.turicum.memory.Context;

public class Constants {

    private final static Object[] values = new Object[]{
            "true", true,
            "false", false,
            "null", null,
    };

    public static void register(Context context) {
        for (int i = 0; i < values.length; i += 2) {
            context.global((String) values[i], values[i + 1]);
            context.freeze((String) values[i]);
        }
    }

}
