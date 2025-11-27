package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.LngCallable;
import ch.turic.SnakeNamed;
import ch.turic.TuriFunction;
import ch.turic.commands.operators.Cast;
import ch.turic.exceptions.ExecutionException;

import java.util.function.*;

/*snippet builtin0165

=== `java_callback`

This function creates a Java callback.
A java_callback can be passed to Java methods as an argument where it expects a `java.util.function.*` type.

To do the conversion, you have to supply a function, closure, or something callable to the function, and then apply
`.as("...")` on the result to get the actual type. For example:

{%S java_callback1%}

Although this example is not the typical use of the function, it demonstrates the conversion.

The parameter of the method `as()` is the simple name of the Java class to convert to.
The implemented conversions are listed below.

{%#replace (regex) ~{%@snip functions_for_callback%}~.*"(.*?)".*~* `$1`~%}

end snippet */
@SnakeNamed.Name("java_callback")
public class JavaCallback implements TuriFunction {
    @Override
    public CallbackConverter call(Context context, Object[] arguments) throws ExecutionException {
        return new CallbackConverter(FunUtils.arg(name(), arguments, LngCallable.class), context);
    }

    public record CallbackConverter(LngCallable callable, Context context) {
        public Object as(String asClass) throws ExecutionException {
            return convert(asClass, callable, context);
        }
    }

    private static Object convert(String asClass, LngCallable callable, Context context) {
        record CallBack(Context context, LngCallable callable) {
            public Object call(Object... args) throws ExecutionException {
                return callable.call(context, args);
            }

            public Boolean callBoolean(Object... args) throws ExecutionException {
                return Cast.toBoolean(callable.call(context, args));
            }

            public Double callDouble(Object... args) throws ExecutionException {
                return Cast.toDouble(callable.call(context, args));
            }

            public Integer callInt(Object... args) throws ExecutionException {
                return Cast.toLong(callable.call(context, args)).intValue();
            }

            public Long callLong(Object... args) throws ExecutionException {
                return Cast.toLong(callable.call(context, args));
            }
        }
        CallBack callBack = new CallBack(context, callable);
        return switch (asClass) {
            // snippet functions_for_callback
            case "BiConsumer" -> (BiConsumer<?, ?>) callBack::call;
            case "BiFunction" -> (BiFunction<?, ?, ?>) callBack::call;
            case "BinaryOperator" -> (BinaryOperator<?>) callBack::call;
            case "BiPredicate" -> (BiPredicate<?, ?>) callBack::callBoolean;
            case "BooleanSupplier" -> (BooleanSupplier) callBack::callBoolean;
            case "Consumer" -> (Consumer<?>) callBack::call;
            case "DoubleBinaryOperator" -> (DoubleBinaryOperator) callBack::callDouble;
            case "DoubleConsumer" -> (DoubleConsumer) callBack::call;
            case "DoubleFunction" -> (DoubleFunction<?>) callBack::call;
            case "DoublePredicate" -> (DoublePredicate) callBack::callBoolean;
            case "DoubleSupplier" -> (DoubleSupplier) callBack::callDouble;
            case "DoubleToIntFunction" -> (DoubleToIntFunction) callBack::callInt;
            case "DoubleToLongFunction" -> (DoubleToLongFunction) callBack::callLong;
            case "DoubleUnaryOperator" -> (DoubleUnaryOperator) callBack::callDouble;
            case "Function" -> (Function<?, ?>) callBack::call;
            case "IntBinaryOperator" -> (IntBinaryOperator) callBack::callInt;
            case "IntConsumer" -> (IntConsumer) callBack::call;
            case "IntFunction" -> (IntFunction<?>) callBack::call;
            case "IntPredicate" -> (IntPredicate) callBack::callBoolean;
            case "IntSupplier" -> (IntSupplier) callBack::callInt;
            case "IntToDoubleFunction" -> (IntToDoubleFunction) callBack::callDouble;
            case "IntToLongFunction" -> (IntToLongFunction) callBack::callLong;
            case "IntUnaryOperator" -> (IntUnaryOperator) callBack::callInt;
            case "LongBinaryOperator" -> (LongBinaryOperator) callBack::callLong;
            case "LongConsumer" -> (LongConsumer) callBack::call;
            case "LongFunction" -> (LongFunction<?>) callBack::call;
            case "LongPredicate" -> (LongPredicate) callBack::callBoolean;
            case "LongSupplier" -> (LongSupplier) callBack::callLong;
            case "LongToDoubleFunction" -> (LongToDoubleFunction) callBack::callDouble;
            case "LongToIntFunction" -> (LongToIntFunction) callBack::callInt;
            case "LongUnaryOperator" -> (LongUnaryOperator) callBack::callLong;
            case "ObjDoubleConsumer" -> (ObjDoubleConsumer<?>) callBack::callDouble;
            case "ObjIntConsumer" -> (ObjIntConsumer<?>) callBack::call;
            case "ObjLongConsumer" -> (ObjLongConsumer<?>) callBack::call;
            case "Predicate" -> (Predicate<?>) callBack::callBoolean;
            case "Supplier" -> (Supplier<?>) callBack::call;
            case "ToDoubleBiFunction" -> (ToDoubleBiFunction<?, ?>) callBack::callDouble;
            case "ToDoubleFunction" -> (ToDoubleFunction<?>) callBack::callDouble;
            case "ToIntBiFunction" -> (ToIntBiFunction<?, ?>) callBack::callInt;
            case "ToIntFunction" -> (ToIntFunction<?>) callBack::callInt;
            case "ToLongBiFunction" -> (ToLongBiFunction<?, ?>) callBack::callLong;
            case "ToLongFunction" -> (ToLongFunction<?>) callBack::callLong;
            case "UnaryOperator" -> (UnaryOperator<?>) callBack::call;
            // end snippet
            default -> throw new ExecutionException("Unsupported conversion target class: %s", asClass);
        };
    }
}