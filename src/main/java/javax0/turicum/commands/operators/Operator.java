package javax0.turicum.commands.operators;

import javax0.turicum.commands.Command;
import javax0.turicum.commands.ExecutionException;
import javax0.turicum.memory.Context;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;

public interface Operator {

    Operator[] OPERATOR_ARRAY = {
            new Add(), new Subtract(), new Multiply(), new Divide(), new Mod(),
            new Compare.Equal(),
            new Compare.LessOrEqual(), new Compare.LessThan(),
            new Compare.GreaterOrEqual(), new Compare.GreaterThan(),
            new And(), new Or(), new Not(), new Pipe(), new RangeOp()
    };

    Map<String, Operator> OPERATORS = register();

    private static Map<String, Operator> register() {
        final Map<String, Operator> map = new HashMap<>();
        for (final var operator : OPERATOR_ARRAY) {
            final var old = map.put(operator.symbol(), operator);
            if (old != null) {
                throw new IllegalStateException("Duplicate operator name: " + operator.symbol());
            }
        }
        return map;
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Symbol {
        String value();
    }

    Object execute(Context ctx, Command left, Command right) throws ExecutionException;

    default String symbol() {
        final var s = this.getClass().getAnnotation(Symbol.class);
        if (s == null) {
            throw new IllegalArgumentException("No name annotation present");
        }
        return s.value();
    }
}
