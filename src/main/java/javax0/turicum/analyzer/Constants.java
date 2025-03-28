package javax0.turicum.analyzer;

import javax0.turicum.memory.Context;
import javax0.turicum.memory.InfinitValue;

public class Constants {

    private final static Object[] values = new Object[]{
            "true", true,
            "false", false,
            "null", null,
            "inf", InfinitValue.INF_POSITIVE
    };

    public static void register(Context context) {
        for (int i = 0; i < values.length; i += 2) {
            context.global((String) values[i], values[i + 1]);
            context.freeze((String) values[i]);
        }
    }

}
