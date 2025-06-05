package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.operators.Cast;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Get the absolute value of the argument
 */
public class MathFunctions {


    public static class Random implements TuriFunction {
        @Override
        public String name() {
            return "random";
        }

        @Override
        public Object call(Context context, Object[] arguments) throws ExecutionException {
            FunUtils.noArg(name(), arguments);
            return Math.random();
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
     * that take a single numeric argument (either long or double) and return a result of the type long.
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
     * on two double precision floating-point inputs. It provides a unified implementation
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

    public static class Sin extends MathFunc1 {
        public Sin() {
            super(Math::sin);
        }

        @Override
        public String name() {
            return "sin";
        }
    }

    public static class Cos extends MathFunc1 {
        public Cos() {
            super(Math::cos);
        }

        @Override
        public String name() {
            return "cos";
        }
    }

    public static class ACos extends MathFunc1 {
        public ACos() {
            super(Math::acos);
        }

        @Override
        public String name() {
            return "acos";
        }
    }

    public static class Asin extends MathFunc1 {
        public Asin() {
            super(Math::asin);
        }

        @Override
        public String name() {
            return "asin";
        }
    }

    public static class Atan extends MathFunc1 {
        public Atan() {
            super(Math::atan);
        }

        @Override
        public String name() {
            return "atan";
        }
    }

    public static class Cbrt extends MathFunc1 {
        public Cbrt() {
            super(Math::cbrt);
        }

        @Override
        public String name() {
            return "cbrt";
        }
    }

    public static class Ceil extends MathFunc1 {
        public Ceil() {
            super(Math::ceil);
        }

        @Override
        public String name() {
            return "ceil";
        }
    }

    public static class Abs extends MathFunc1 {
        public Abs() {
            super(Math::abs);
        }

        @Override
        public String name() {
            return "abs";
        }
    }

    public static class Exp extends MathFunc1 {
        public Exp() {
            super(Math::exp);
        }

        @Override
        public String name() {
            return "exp";
        }
    }

    public static class Floor extends MathFunc1 {
        public Floor() {
            super(Math::floor);
        }

        @Override
        public String name() {
            return "floor";
        }
    }

    public static class Log extends MathFunc1 {
        public Log() {
            super(Math::log);
        }

        @Override
        public String name() {
            return "log";
        }
    }

    public static class Log10 extends MathFunc1 {
        public Log10() {
            super(Math::log10);
        }

        @Override
        public String name() {
            return "log10";
        }
    }

    public static class Sqrt extends MathFunc1 {
        public Sqrt() {
            super(Math::sqrt);
        }

        @Override
        public String name() {
            return "sqrt";
        }
    }

    public static class Tan extends MathFunc1 {
        public Tan() {
            super(Math::tan);
        }

        @Override
        public String name() {
            return "tan";
        }
    }

    public static class Tanh extends MathFunc1 {
        public Tanh() {
            super(Math::tanh);
        }

        @Override
        public String name() {
            return "tanh";
        }
    }

    public static class Sinh extends MathFunc1 {
        public Sinh() {
            super(Math::sinh);
        }

        @Override
        public String name() {
            return "sinh";
        }
    }

    public static class Cosh extends MathFunc1 {
        public Cosh() {
            super(Math::cosh);
        }

        @Override
        public String name() {
            return "cosh";
        }
    }

    public static class GetExponent extends MathFunc1i {
        public GetExponent() {
            super(Math::getExponent);
        }

        @Override
        public String name() {
            return "get_exponent";
        }
    }


    public static class Round extends MathFunc1l {
        public Round() {
            super(Math::round);
        }

        @Override
        public String name() {
            return "round";
        }
    }

    public static class SigNum extends MathFunc1 {
        public SigNum() {
            super(Math::signum);
        }

        @Override
        public String name() {
            return "signum";
        }
    }

    public static class NextDown extends MathFunc1 {
        public NextDown() {
            super(Math::nextDown);
        }

        @Override
        public String name() {
            return "next_down";
        }
    }

    public static class Ulp extends MathFunc1 {
        public Ulp() {
            super(Math::ulp);
        }

        @Override
        public String name() {
            return "ulp";
        }
    }

    public static class Rint extends MathFunc1 {
        public Rint() {
            super(Math::rint);
        }

        @Override
        public String name() {
            return "rint";
        }
    }

    public static class ToDegrees extends MathFunc1 {
        public ToDegrees() {
            super(Math::toDegrees);
        }

        @Override
        public String name() {
            return "to_degrees";
        }
    }

    public static class ToRadians extends MathFunc1 {
        public ToRadians() {
            super(Math::toRadians);
        }

        @Override
        public String name() {
            return "to_radians";
        }
    }

    public static class Pow extends MathFunc2 {
        public Pow() {
            super(Math::pow);
        }

        @Override
        public String name() {
            return "pow";
        }
    }

    public static class Atan2 extends MathFunc2 {
        public Atan2() {
            super(Math::atan2);
        }

        @Override
        public String name() {
            return "atan2";
        }
    }

    public static class IEEERemainder extends MathFunc2 {
        public IEEERemainder() {
            super(Math::IEEEremainder);
        }

        @Override
        public String name() {
            return "ieee_remainder";
        }
    }

    public static class CopySign extends MathFunc2 {
        public CopySign() {
            super(Math::copySign);
        }

        @Override
        public String name() {
            return "copy_sign";
        }
    }

    public static class Hypot extends MathFunc2 {
        public Hypot() {
            super(Math::hypot);
        }

        @Override
        public String name() {
            return "hypot";
        }
    }

    public static class NextAfter extends MathFunc2 {
        public NextAfter() {
            super(Math::nextAfter);
        }

        @Override
        public String name() {
            return "next_after";
        }
    }

    public static class IEEEremainder extends MathFunc2 {
        public IEEEremainder() {
            super(Math::IEEEremainder);
        }

        @Override
        public String name() {
            return "ieee_remainder";
        }
    }

    public static class NextUp extends MathFunc1 {
        public NextUp() {
            super(Math::nextUp);
        }

        @Override
        public String name() {
            return "next_up";
        }
    }

}