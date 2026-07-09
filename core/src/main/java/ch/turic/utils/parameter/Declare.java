package ch.turic.utils.parameter;

import ch.turic.Command;
import ch.turic.Input;
import ch.turic.analyzer.ExpressionAnalyzer;
import ch.turic.analyzer.Lexer;
import ch.turic.analyzer.Pos;
import ch.turic.analyzer.Types;
import ch.turic.commands.ConstantExpression;
import ch.turic.commands.ParameterList;
import ch.turic.commands.TypeDeclaration;

import java.util.Arrays;

/**
 * Fluent factory for building {@link ParameterList} instances directly from Java code.
 * <p>
 * This is the programmatic equivalent of declaring a Turicum parameter list through
 * {@link ch.turic.TuriParameters}. It is useful for Java-coded built-ins whose signature
 * should participate in normal Turicum argument binding, but where an annotation is not
 * convenient or the parameter list is easier to keep close to executable Java code.
 * <p>
 * Example:
 * <pre>{@code
 * private final ParameterList parameters = Declare.params(
 *         param("object").any().mandatory(),
 *         param("tab").integer().defaultValue(4),
 *         param("margin").integer().defaultValue(60)
 * ).done();
 *
 * @Override
 * public ParameterList parameters() {
 *     return parameters;
 * }
 * }</pre>
 * <p>
 * Instances are immutable. Each modifier method returns a new builder with the requested
 * change applied.
 */
public class Declare {

    private final ParameterList.Parameter[] parameters;
    private final String rest;
    private final String meta;
    private final String closure;
    private final Pos position;

    private Declare(ParameterList.Parameter[] parameters, String rest, String meta, String closure, Pos position) {
        this.parameters = parameters;
        this.rest = rest;
        this.meta = meta;
        this.closure = closure;
        this.position = position;
    }

    /**
     * Starts a parameter-list declaration with the regular parameters.
     *
     * @param parameters the positional, named, or positional-or-named parameters
     * @return a builder that can be completed with {@link #done()} or extended with
     * {@link #rest(String)}, {@link #meta(String)}, and {@link #closure(String)}
     */
    public static Declare params(ParameterList.Parameter... parameters) {
        return new Declare(parameters, null, null, null, null);
    }

    /**
     * Adds the rest parameter name.
     * <p>
     * Extra positional arguments will be collected into this parameter as an {@code LngList}.
     *
     * @param rest the rest parameter name, without square brackets
     * @return a new builder with the rest parameter set
     */
    public Declare rest(String rest) {
        return new Declare(parameters, rest, meta, closure, position);
    }

    /**
     * Adds the meta parameter name.
     * <p>
     * Extra named arguments will be collected into this parameter as an {@code LngObject}.
     *
     * @param meta the meta parameter name, without braces
     * @return a new builder with the meta parameter set
     */
    public Declare meta(String meta) {
        return new Declare(parameters, rest, meta, closure, position);
    }

    /**
     * Adds the trailing closure parameter name.
     * <p>
     * When the call syntax provides a trailing closure, the argument binder stores it
     * under this name.
     *
     * @param closure the closure parameter name, without the {@code ^} prefix
     * @return a new builder with the closure parameter set
     */
    public Declare closure(String closure) {
        return new Declare(parameters, rest, meta, closure, position);
    }

    /**
     * Finishes the declaration.
     *
     * @return the immutable parameter-list descriptor used by callable objects
     */
    public ParameterList done() {
        return new ParameterList(parameters, rest, meta, closure, position);
    }

    /**
     * Builder for a single regular parameter.
     * <p>
     * Use {@link #param(String)} to start with a positional-or-named mandatory parameter,
     * then apply modifiers such as {@link #named()}, {@link #positional()}, {@link #type(String...)},
     * fluent type helpers like {@link #str()} and {@link #integer()}, {@link #mandatory()},
     * {@link #defaultExpression(String)}, or {@link #defaultValue(Object)}.
     */
    public static class Parameter {
        final String identifier;
        final ParameterList.Parameter.Type type;
        final TypeDeclaration[] types;
        final Command defaultExpression;
        private static final TypeDeclaration[] EMPTY_TYPE = new TypeDeclaration[0];


        public Parameter(String identifier, ParameterList.Parameter.Type type, TypeDeclaration[] types, Command defaultExpression) {
            this.identifier = identifier;
            this.type = type;
            this.types = types;
            this.defaultExpression = defaultExpression;
        }

        private Parameter(String identifier) {
            this(identifier, ParameterList.Parameter.Type.POSITIONAL_OR_NAMED, EMPTY_TYPE, null);
        }

        /**
         * Starts a regular parameter declaration.
         *
         * @param identifier the parameter name
         * @return a parameter builder for a positional-or-named parameter
         */
        public static Parameter param(String identifier) {
            return new Parameter(identifier);
        }

        /**
         * Marks the parameter as named-only, equivalent to the {@code @name} syntax.
         *
         * @return a new parameter builder with the named-only modifier applied
         */
        public Parameter named() {
            return new Parameter(identifier, ParameterList.Parameter.Type.NAMED_ONLY, types, defaultExpression);
        }

        /**
         * Marks the parameter as positional-only, equivalent to the {@code !name} syntax.
         *
         * @return a new parameter builder with the positional-only modifier applied
         */
        public Parameter positional() {
            return new Parameter(identifier, ParameterList.Parameter.Type.POSITIONAL_ONLY, types, defaultExpression);
        }

        /**
         * Adds accepted type names to the parameter.
         * <p>
         * The strings are the same type names used in Turicum source declarations, for example
         * {@code "str"}, {@code "int"}, {@code "num"}, or {@code "any"}.
         *
         * @param types accepted type names
         * @return a new parameter builder with the type declarations applied
         */
        public Parameter type(String... types) {
            final var typeDeclarations = Arrays.stream(types).map(s -> new TypeDeclaration(s, null)).toArray(TypeDeclaration[]::new);
            return new Parameter(identifier, type, typeDeclarations, defaultExpression);
        }

        /**
         * Readability helper for type alternatives.
         * <p>
         * This method does not change the parameter; it allows declarations such as
         * {@code param("path").str().or().none()}.
         *
         * @return this parameter builder
         */
        public Parameter or() {
            return this;
        }

        /**
         * Adds the {@code any} type to the accepted type alternatives.
         *
         * @return a new parameter builder with the type appended
         */
        public Parameter any() {
            return addType(Types.ANY);
        }

        /**
         * Adds the {@code bool} type to the accepted type alternatives.
         *
         * @return a new parameter builder with the type appended
         */
        public Parameter bool() {
            return addType(Types.BOOL);
        }

        /**
         * Adds the {@code macro} type to the accepted type alternatives.
         *
         * @return a new parameter builder with the type appended
         */
        public Parameter macro() {
            return addType(Types.MACRO);
        }

        /**
         * Adds the {@code cls} type to the accepted type alternatives.
         *
         * @return a new parameter builder with the type appended
         */
        public Parameter cls() {
            return addType(Types.CLS);
        }

        /**
         * Adds the {@code lst} type to the accepted type alternatives.
         *
         * @return a new parameter builder with the type appended
         */
        public Parameter lst() {
            return addType(Types.LST);
        }

        /**
         * Adds the {@code str} type to the accepted type alternatives.
         *
         * @return a new parameter builder with the type appended
         */
        public Parameter str() {
            return addType(Types.STR);
        }

        /**
         * Adds the {@code float} type to the accepted type alternatives.
         *
         * @return a new parameter builder with the type appended
         */
        public Parameter floating() {
            return addType(Types.FLOAT);
        }

        /**
         * Adds the {@code int} type to the accepted type alternatives.
         *
         * @return a new parameter builder with the type appended
         */
        public Parameter integer() {
            return addType(Types.INT);
        }

        /**
         * Adds the {@code num} type to the accepted type alternatives.
         *
         * @return a new parameter builder with the type appended
         */
        public Parameter num() {
            return addType(Types.NUM);
        }

        /**
         * Adds the {@code err} type to the accepted type alternatives.
         *
         * @return a new parameter builder with the type appended
         */
        public Parameter err() {
            return addType(Types.ERR);
        }

        /**
         * Adds the {@code obj} type to the accepted type alternatives.
         *
         * @return a new parameter builder with the type appended
         */
        public Parameter obj() {
            return addType(Types.OBJ);
        }

        /**
         * Adds the {@code fn} type to the accepted type alternatives.
         *
         * @return a new parameter builder with the type appended
         */
        public Parameter fn() {
            return addType(Types.FN);
        }

        /**
         * Adds the {@code que} type to the accepted type alternatives.
         *
         * @return a new parameter builder with the type appended
         */
        public Parameter que() {
            return addType(Types.QUE);
        }

        /**
         * Adds the {@code task} type to the accepted type alternatives.
         *
         * @return a new parameter builder with the type appended
         */
        public Parameter task() {
            return addType(Types.TASK);
        }

        /**
         * Adds the {@code mtx} type to the accepted type alternatives.
         *
         * @return a new parameter builder with the type appended
         */
        public Parameter mtx() {
            return addType(Types.MTX);
        }

        /**
         * Adds the {@code atm} type to the accepted type alternatives.
         *
         * @return a new parameter builder with the type appended
         */
        public Parameter atm() {
            return addType(Types.ATM);
        }

        /**
         * Adds the {@code none} type to the accepted type alternatives.
         *
         * @return a new parameter builder with the type appended
         */
        public Parameter none() {
            return addType(Types.NONE);
        }

        /**
         * Adds the {@code some} type to the accepted type alternatives.
         *
         * @return a new parameter builder with the type appended
         */
        public Parameter some() {
            return addType(Types.SOME);
        }

        private Parameter addType(String typeName) {
            final var typeDeclarations = Arrays.copyOf(types, types.length + 1);
            typeDeclarations[types.length] = new TypeDeclaration(typeName, null);
            return new Parameter(identifier, type, typeDeclarations, defaultExpression);
        }

        /**
         * Finishes the parameter with a default expression.
         * <p>
         * The expression is parsed using the normal Turicum expression analyzer and evaluated
         * by the argument binder when the caller omits this parameter.
         *
         * @param expression Turicum source code for the default expression
         * @return the completed parameter descriptor
         */
        public ParameterList.Parameter defaultExpression(String expression) {
            final var input = Input.fromString(expression, "parameter_expression");
            final var tokens = Lexer.analyze(input);
            final var command = ExpressionAnalyzer.INSTANCE.analyze(tokens);
            if (!tokens.isEmpty()) {
                throw tokens.syntaxError("Extra token '%s' after parameter default expression", tokens.peek().text());
            }
            return new ParameterList.Parameter(identifier, type, types, command);
        }

        /**
         * Finishes the parameter with a constant integer default value.
         * <p>
         * Turicum represents integer values as Java {@code Long}, so the stored default is widened
         * before it is wrapped in a command.
         *
         * @param value the default integer value
         * @return the completed parameter descriptor
         */
        public ParameterList.Parameter defaultValue(int value) {
            return defaultCommand(new ConstantExpression((long) value));
        }

        /**
         * Finishes the parameter with a constant integer default value.
         *
         * @param value the default integer value
         * @return the completed parameter descriptor
         */
        public ParameterList.Parameter defaultValue(long value) {
            return defaultCommand(new ConstantExpression(value));
        }

        /**
         * Finishes the parameter with a constant floating-point default value.
         * <p>
         * Turicum represents floating-point values as Java {@code Double}, so the stored default
         * is widened before it is wrapped in a command.
         *
         * @param value the default floating-point value
         * @return the completed parameter descriptor
         */
        public ParameterList.Parameter defaultValue(float value) {
            return defaultCommand(new ConstantExpression((double) value));
        }

        /**
         * Finishes the parameter with a constant floating-point default value.
         *
         * @param value the default floating-point value
         * @return the completed parameter descriptor
         */
        public ParameterList.Parameter defaultValue(double value) {
            return defaultCommand(new ConstantExpression(value));
        }

        /**
         * Finishes the parameter with a constant boolean default value.
         *
         * @param value the default boolean value
         * @return the completed parameter descriptor
         */
        public ParameterList.Parameter defaultValue(boolean value) {
            return defaultCommand(new ConstantExpression(value));
        }

        /**
         * Finishes the parameter with a constant string default value.
         * <p>
         * The string is stored directly. It is not parsed as a Turicum string literal and it
         * does not perform interpolation.
         *
         * @param value the default string value
         * @return the completed parameter descriptor
         */
        public ParameterList.Parameter defaultValue(String value) {
            return defaultCommand(new ConstantExpression(value));
        }

        /**
         * Finishes the parameter with a constant {@code none} default value.
         *
         * @return the completed parameter descriptor
         */
        public ParameterList.Parameter defaultNone() {
            return defaultCommand(new ConstantExpression(null));
        }

        /**
         * Finishes the parameter with an arbitrary constant Java value.
         * <p>
         * Prefer the primitive overloads for Turicum numbers, so the default value has the
         * same Java representation as a value parsed from source.
         *
         * @param value the default value
         * @return the completed parameter descriptor
         */
        public ParameterList.Parameter defaultValue(Object value) {
            return defaultCommand(new ConstantExpression(value));
        }

        /**
         * Finishes the parameter with an already-built command used as the default expression.
         *
         * @param command the command to execute when the caller omits this parameter
         * @return the completed parameter descriptor
         */
        public ParameterList.Parameter defaultCommand(Command command) {
            return new ParameterList.Parameter(identifier, type, types, command);
        }

        /**
         * Finishes the parameter without a default expression.
         *
         * @return the completed mandatory parameter descriptor
         */
        public ParameterList.Parameter mandatory() {
            return new ParameterList.Parameter(identifier, type, types, defaultExpression);
        }
    }
}
