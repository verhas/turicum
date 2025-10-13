package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.SnakeNamed.Name;
import ch.turic.TuriFunction;
import ch.turic.commands.operators.Cast;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Get the absolute value of the argument
 */
public class MathFunctions {

    // snippet math_functions_doc

    /**
     * . * `random()` Returns a random number between 0 and 1.
     */
    public static class Random implements TuriFunction {

        @Override
        public Object call(Context context, Object[] arguments) throws ExecutionException {
            FunUtils.noArg(name(), arguments);
            return Math.random();
        }
    }

    /**
     * . * `java_long(x)` Converts the argument to a Java long.
     * . Technically, this is the same function as `int` because Turicum stores integer numbers as longs.
     * . The use of this function is recommended over `int` when the argument is used to pass to a Java method that expects a long argument.
     */
    @Name("java_long")
    public static class ToJavaLong extends ToInt {

    }

    /**
     * . * `int(x)` Converts the argument to an integer.
     */
    @Name("int")
    public static class ToInt implements TuriFunction {
        @Override
        public Object call(Context context, Object[] arguments) throws ExecutionException {
            final var arg = FunUtils.arg(name(), arguments);
            if (Cast.isLong(arg)) {
                return Cast.toLong(arg);
            }
            if (Cast.isDouble(arg)) {
                return Cast.toDouble(arg).longValue();
            }
            throw new ExecutionException("%s argument is not a long/double", name());
        }
    }

    /**
     * . * `java_int(x)` Converts the argument to a Java Integer.
     * . Use this function when the argument is used to pass to a Java method that expects an int or Integer argument.
     */
    @Name("java_int")
    public static class ToJavaInt implements TuriFunction {
        @Override
        public Object call(Context context, Object[] arguments) throws ExecutionException {
            final var arg = FunUtils.arg(name(), arguments);
            if (Cast.isLong(arg)) {
                return Cast.toLong(arg).intValue();
            }
            if (Cast.isDouble(arg)) {
                return Cast.toDouble(arg).intValue();
            }
            throw new ExecutionException("%s argument is not a long/double", name());
        }
    }

    /**
     * . * `java_short(x)` Converts the argument to a Java Short.
     * . Use this function when the argument is used to pass to a Java method that expects a short or Short argument.
     */
    @Name("java_short")
    public static class ToJavaShort implements TuriFunction {
        @Override
        public Object call(Context context, Object[] arguments) throws ExecutionException {
            final var arg = FunUtils.arg(name(), arguments);
            if (Cast.isLong(arg)) {
                return Cast.toLong(arg).shortValue();
            }
            if (Cast.isDouble(arg)) {
                return Cast.toDouble(arg).shortValue();
            }
            throw new ExecutionException("%s argument is not a long/double", name());
        }
    }

    /**
     * . * `java_char(x)` Converts the argument to a Java Character.
     * . Use this function when the argument is used to pass to a Java method that expects a char or Character argument.
     * . Note that some of the numerical values interpreted as code points may not be valid characters.
     * . Multiple Java characters may represent them.
     * . In such cases, an error is thrown.
     */
    @Name("java_char")
    public static class ToJavaChar implements TuriFunction {
        @Override
        public Object call(Context context, Object[] arguments) throws ExecutionException {
            final var arg = FunUtils.arg(name(), arguments);
            final char[] chs;
            if (Cast.isLong(arg)) {
                chs = Character.toChars(Cast.toLong(arg).intValue());
            } else if (Cast.isDouble(arg)) {
                chs = Character.toChars(Cast.toDouble(arg).intValue());
            } else {
                throw new ExecutionException("%s argument is not a long/double", name());
            }
            if (chs.length > 1) {
                throw new ExecutionException("%s argument cannot be represented by a single char", name());
            }
            return chs[0];
        }
    }

    /**
     * . * `java_byte(x)` Converts the argument to a Java Byte.
     * . Use this function when the argument is used to pass to a Java method that expects a byte or Byte argument.
     */
    @Name("java_byte")
    public static class ToJavaByte implements TuriFunction {
        @Override
        public Object call(Context context, Object[] arguments) throws ExecutionException {
            final var arg = FunUtils.arg(name(), arguments);
            if (Cast.isLong(arg)) {
                return Cast.toLong(arg).byteValue();
            }
            if (Cast.isDouble(arg)) {
                return Cast.toDouble(arg).byteValue();
            }
            throw new ExecutionException("%s argument is not a long/double", name());
        }
    }

    /**
     * . * `float(x)` Converts the argument to a float.
     * . The actual value is represented as a double.
     * . Turicum stores floating numbers internally as doubles.
     */
    @Name("float")
    public static class ToFloat implements TuriFunction {
        @Override
        public Object call(Context context, Object[] arguments) throws ExecutionException {
            final var arg = FunUtils.arg(name(), arguments);
            if (Cast.isLong(arg)) {
                return Cast.toLong(arg).doubleValue();
            }
            if (Cast.isDouble(arg)) {
                return Cast.toDouble(arg);
            }
            throw new ExecutionException("%s argument is not a long/double", name());
        }
    }

    /**
     * . * `java_double(x)` Converts the argument to a Java double.
     * . Use this function when the argument is used to pass to a Java method that expects a double or Double argument.
     * . Turicum stores floating numbers internally as doubles.
     * . So technically this function is the same as `float`.
     */
    @Name("java_double")
    public static class ToJavaDouble extends ToFloat {

    }

    /**
     * . * `java_float(x)` Converts the argument to a Java float.
     * . Use this function when the argument is used to pass to a Java method that expects a float or Float argument.
     */
    @Name("java_float")
    public static class ToJavaFloat implements TuriFunction {
        @Override
        public Object call(Context context, Object[] arguments) throws ExecutionException {
            final var arg = FunUtils.arg(name(), arguments);
            if (Cast.isLong(arg)) {
                return Cast.toLong(arg).floatValue();
            }
            if (Cast.isDouble(arg)) {
                return Cast.toDouble(arg).floatValue();
            }
            throw new ExecutionException("%s argument is not a long/double", name());
        }
    }

    /**
     * . * `num(x)` Converts the argument to a number.
     * . The result will either be a float or an int (Java double or long).
     */
    @Name("num")
    public static class ToNumber implements TuriFunction {
        @Override
        public Object call(Context context, Object[] arguments) throws ExecutionException {
            final var arg = FunUtils.arg(name(), arguments);
            if (Cast.isLong(arg)) {
                return Cast.toLong(arg);
            }
            if (Cast.isDouble(arg)) {
                return Cast.toDouble(arg);
            }
            throw new ExecutionException("%s argument is not a long/double", name());
        }
    }

    private static abstract class MathFunc1 implements TuriFunction {
        private final Function<Double, Double> functions;

        private MathFunc1(Function<Double, Double> functions) {
            this.functions = functions;
        }

        @Override
        public Object call(Context context, Object[] arguments) throws ExecutionException {
            final var arg = FunUtils.arg(name(), arguments);
            if (Cast.isDouble(arg)) {
                return functions.apply(Cast.toDouble(arg));
            }
            throw new ExecutionException("%s argument is not a long/double", name());
        }
    }

    /**
     * Abstract base class representing a mathematical function that processes
     * a single numerical argument and produces an integer result.
     *
     * <p>This class implements the {@code TuriFunction} interface, enabling it to
     * be used within the Turi language system. It provides a mechanism to execute
     * a predefined function on numeric arguments (either {@code long} or {@code double})
     * and return the result as an {@code Integer}.
     *
     * <p>Subclasses must specify the mathematical function applied by passing a
     * {@code Function<Double, Integer>} to the constructor. Derived classes must also
     * implement the {@code name()} method to provide a unique identifier for the function.
     *
     * <p>Methods include:
     * - {@code call(Context context, Object[] arguments)} for evaluating the
     * function on provided arguments.
     */
    private static abstract class MathFunc1i implements TuriFunction {
        private final Function<Double, Integer> functions;

        private MathFunc1i(Function<Double, Integer> functions) {
            this.functions = functions;
        }

        @Override
        public Integer call(Context context, Object[] arguments) throws ExecutionException {
            final var arg = FunUtils.arg(name(), arguments);
            if (Cast.isDouble(arg)) {
                return functions.apply(Cast.toDouble(arg));
            } else {
                throw new ExecutionException("%s argument is not a long/double", name());
            }
        }
    }

    /**
     * Represents an abstract base class for mathematical functions in the Turi language
     * that takes a single numeric argument (either long or double) and returns a result of the type long.
     * This class serves as a utility for defining mathematical operations that conform
     * to the TuriFunction interface.
     * <p>
     * Subclasses should define specific mathematical transformations by providing a unique
     * implementation for the `name` method to identify the function within the Turi environment.
     */
    private static abstract class MathFunc1l implements TuriFunction {
        private final Function<Double, Long> functions;

        private MathFunc1l(Function<Double, Long> functions) {
            this.functions = functions;
        }

        @Override
        public Long call(Context context, Object[] arguments) throws ExecutionException {
            final var arg = FunUtils.arg(name(), arguments);
            if (Cast.isDouble(arg)) {
                return functions.apply(Cast.toDouble(arg));
            }
            throw new ExecutionException("%s argument is not a long/double", name());
        }
    }

    /**
     * An abstract base class representing a mathematical binary function operating
     * on two double-precision floating-point inputs. It provides a unified implementation
     * for calling specific mathematical operations using a provided BiFunction, making
     * it easier to define and manage mathematical operations consistently.
     * <p>
     * Subclasses of MathFunc2 must provide a specific calculation by passing a BiFunction
     * for the desired mathematical operation and implement the abstract {@code name()} method
     * to define the identifier name of the function.
     * <p>
     * This class implements the TuriFunction interface, enabling it to be used as a callable
     * function within the Turi language system.
     */
    private static abstract class MathFunc2 implements TuriFunction {
        private final BiFunction<Double, Double, Double> functions;

        private MathFunc2(BiFunction<Double, Double, Double> functions) {
            this.functions = functions;
        }

        @Override
        public Object call(Context context, Object[] arguments) throws ExecutionException {
            final var args = FunUtils.args(name(), arguments, FunUtils.ArgumentsHolder.LngNumber.class, FunUtils.ArgumentsHolder.LngNumber.class);
            final var a = args.at(0).doubleValue();
            final var b = args.at(1).doubleValue();
            return functions.apply(a, b);
        }
    }

    /**
     * . * `scalb(d,s)` returns `d` times `2` to the power of `s`.
     */
    public static class Scalb implements TuriFunction {
        @Override
        public String name() {
            return "scalb";
        }

        @Override
        public Object call(Context context, Object[] arguments) throws ExecutionException {
            final var args = FunUtils.args(name(), arguments, FunUtils.ArgumentsHolder.LngNumber.class, FunUtils.ArgumentsHolder.LngLong.class);
            final var a = args.at(0).doubleValue();
            final var b = args.at(1).intValue();
            return Math.scalb(a, b);
        }
    }

    /**
     * . *`sin` returns the trigonometric sine of an angle.
     */
    public static class Sin extends MathFunc1 {
        public Sin() {
            super(Math::sin);
        }

        @Override
        public String name() {
            return "sin";
        }
    }

    /**
     * . * `cos(x)` returns the trigonometric cosine of an angle.
     */
    public static class Cos extends MathFunc1 {
        public Cos() {
            super(Math::cos);
        }

        @Override
        public String name() {
            return "cos";
        }
    }

    /**
     * . * `acos(x)` returns the trigonometric arc cosine of a value.
     */
    public static class ACos extends MathFunc1 {
        public ACos() {
            super(Math::acos);
        }

        @Override
        public String name() {
            return "acos";
        }
    }

    /**
     * . * `asin(x)` returns the trigonometric arc sine of a value.
     */
    public static class Asin extends MathFunc1 {
        public Asin() {
            super(Math::asin);
        }

        @Override
        public String name() {
            return "asin";
        }
    }

    /**
     * . * `atan(x)` returns the trigonometric arc tangent of a value.
     */
    public static class Atan extends MathFunc1 {
        public Atan() {
            super(Math::atan);
        }

        @Override
        public String name() {
            return "atan";
        }
    }

    /**
     * . * `cbrt(x)` returns the cube root of a value.
     */
    public static class Cbrt extends MathFunc1 {
        public Cbrt() {
            super(Math::cbrt);
        }

        @Override
        public String name() {
            return "cbrt";
        }
    }

    /**
     * . * `ceil(x)` returns the smallest integer value that is greater than or equal to the argument and is equal to a mathematical integer.
     */
    public static class Ceil extends MathFunc1 {
        public Ceil() {
            super(Math::ceil);
        }

        @Override
        public String name() {
            return "ceil";
        }
    }

    /**
     * . * `abs(x)` returns the absolute value of a number.
     */
    public static class Abs extends MathFunc1 {
        public Abs() {
            super(Math::abs);
        }

        @Override
        public String name() {
            return "abs";
        }
    }

    /**
     * . * `exp(x)` returns the value `e` raised to the power of a value.
     */
    public static class Exp extends MathFunc1 {
        public Exp() {
            super(Math::exp);
        }

        @Override
        public String name() {
            return "exp";
        }
    }

    /**
     * . * `floor(x)` returns the largest integer value that is less than or equal to the argument and is equal to a mathematical integer.
     */
    public static class Floor extends MathFunc1 {
        public Floor() {
            super(Math::floor);
        }

        @Override
        public String name() {
            return "floor";
        }
    }

    /**
     * . * `log(x)` returns the natural logarithm (base `e`) of a value.
     */
    public static class Log extends MathFunc1 {
        public Log() {
            super(Math::log);
        }

        @Override
        public String name() {
            return "log";
        }
    }

    /**
     * . * `log10(x)` returns the base-10 logarithm of a value.
     */
    public static class Log10 extends MathFunc1 {
        public Log10() {
            super(Math::log10);
        }

        @Override
        public String name() {
            return "log10";
        }
    }

    /**
     * . * `sqrt(x)` returns the positive square root of a value.
     */
    public static class Sqrt extends MathFunc1 {
        public Sqrt() {
            super(Math::sqrt);
        }

        @Override
        public String name() {
            return "sqrt";
        }
    }

    /**
     * . * `tan(x)` returns the trigonometric tangent of an angle.
     */
    public static class Tan extends MathFunc1 {
        public Tan() {
            super(Math::tan);
        }

        @Override
        public String name() {
            return "tan";
        }
    }

    /**
     * . * `tanh(x)` returns the hyperbolic tangent of a value.
     */
    public static class Tanh extends MathFunc1 {
        public Tanh() {
            super(Math::tanh);
        }

        @Override
        public String name() {
            return "tanh";
        }
    }

    /**
     * . * `sinh(x)` returns the hyperbolic sine of a value.
     */
    public static class Sinh extends MathFunc1 {
        public Sinh() {
            super(Math::sinh);
        }

        @Override
        public String name() {
            return "sinh";
        }
    }

    /**
     * . * `cosh(x)` returns the hyperbolic cosine of a value.
     */
    public static class Cosh extends MathFunc1 {
        public Cosh() {
            super(Math::cosh);
        }

        @Override
        public String name() {
            return "cosh";
        }
    }

    /**
     * . * `get_exponent(x)` returns the exponent used in the representation of a number.
     */
    public static class GetExponent extends MathFunc1i {
        public GetExponent() {
            super(Math::getExponent);
        }

        @Override
        public String name() {
            return "get_exponent";
        }
    }


    /**
     * . * `round(x)` rounds a number to the nearest integer.
     */
    public static class Round extends MathFunc1l {
        public Round() {
            super(Math::round);
        }

        @Override
        public String name() {
            return "round";
        }
    }

    /**
     * . * `signum(x)` returns the sign of a number.
     */
    public static class SigNum extends MathFunc1 {
        public SigNum() {
            super(Math::signum);
        }

        @Override
        public String name() {
            return "signum";
        }
    }

    /**
     * . * `next_down(x)` returns the next representable floating-point value after a given number towards negative infinity.
     */
    public static class NextDown extends MathFunc1 {
        public NextDown() {
            super(Math::nextDown);
        }

        @Override
        public String name() {
            return "next_down";
        }
    }

    /**
     * . * `ulp(x)` returns the size of an ulp of the argument.
     * . An ulp, unit in the last place, of a double value is the positive distance between this floating-point value and the double value next larger in magnitude.
     */
    public static class Ulp extends MathFunc1 {
        public Ulp() {
            super(Math::ulp);
        }

        @Override
        public String name() {
            return "ulp";
        }
    }

    /**
     * . * `rint(x)` returns the floating-point value that is closest in value to the argument and is equal to a mathematical integer.
     */
    public static class Rint extends MathFunc1 {
        public Rint() {
            super(Math::rint);
        }

        @Override
        public String name() {
            return "rint";
        }
    }

    /**
     * . * `to_degrees(x)` converts an angle measured in radians to an approximately equivalent angle measured in degrees.
     */
    public static class ToDegrees extends MathFunc1 {
        public ToDegrees() {
            super(Math::toDegrees);
        }

        @Override
        public String name() {
            return "to_degrees";
        }
    }

    /**
     * . * `to_radians(x)` converts an angle measured in degrees to an approximately equivalent angle measured in radians.
     */
    public static class ToRadians extends MathFunc1 {
        public ToRadians() {
            super(Math::toRadians);
        }

        @Override
        public String name() {
            return "to_radians";
        }
    }

    /**
     * . * `pow(x,y)` returns the first argument raised to the power of the second argument.
     */
    public static class Pow extends MathFunc2 {
        public Pow() {
            super(Math::pow);
        }

        @Override
        public String name() {
            return "pow";
        }
    }

    /**
     * . * `atan2(x,y)` returns the angle theta from the conversion of rectangular coordinates (x, y) to polar coordinates (r, theta).
     */
    public static class Atan2 extends MathFunc2 {
        public Atan2() {
            super(Math::atan2);
        }

        @Override
        public String name() {
            return "atan2";
        }
    }

    /**
     * . * `ieee_remainder(x,y)` returns the remainder operation on floating-point values according to IEEE 754.
     */
    public static class IEEERemainder extends MathFunc2 {
        public IEEERemainder() {
            super(Math::IEEEremainder);
        }

        @Override
        public String name() {
            return "ieee_remainder";
        }
    }

    /**
     * . * `copy_sign(x,y)` returns a value with the magnitude of the first argument and the sign of the second argument.
     */
    public static class CopySign extends MathFunc2 {
        public CopySign() {
            super(Math::copySign);
        }

        @Override
        public String name() {
            return "copy_sign";
        }
    }

    /**
     * . * `hypot(x,y)` returns the hypotenuse of a right triangle with sides of length a and b.
     */
    public static class Hypot extends MathFunc2 {
        public Hypot() {
            super(Math::hypot);
        }

        @Override
        public String name() {
            return "hypot";
        }
    }

    /**
     * . * `next_after(x,y)` returns the next representable floating-point value after a given number towards positive infinity.
     */
    public static class NextAfter extends MathFunc2 {
        public NextAfter() {
            super(Math::nextAfter);
        }

        @Override
        public String name() {
            return "next_after";
        }
    }

    /**
     * . * `next_up(x)` returns the next representable floating-point value after a given number towards positive infinity.
     */
    public static class NextUp extends MathFunc1 {
        public NextUp() {
            super(Math::nextUp);
        }

        @Override
        public String name() {
            return "next_up";
        }
    }
    // end snippet math_functions_doc
}