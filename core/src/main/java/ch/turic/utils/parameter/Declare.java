package ch.turic.utils.parameter;

import ch.turic.Command;
import ch.turic.Input;
import ch.turic.analyzer.ExpressionAnalyzer;
import ch.turic.analyzer.Lexer;
import ch.turic.analyzer.Pos;
import ch.turic.commands.ParameterList;
import ch.turic.commands.TypeDeclaration;

import java.util.Arrays;

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


    public static Declare params(ParameterList.Parameter ... parameters) {
        return new Declare(parameters, null, null, null, null);
    }

    public Declare rest(String rest) {
        return new Declare(parameters, rest, meta, closure, position);
    }
    public Declare meta(String meta) {
        return new Declare(parameters, rest, meta, closure, position);
    }

    public Declare closure(String closure) {
        return new Declare(parameters, rest, meta, closure, position);
    }

    public ParameterList done(){
        return new ParameterList(parameters, rest, meta, closure, position);
    }

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

        public static Parameter param(String identifier) {
            return new Parameter(identifier);
        }

        public Parameter named() {
            return new Parameter(identifier, ParameterList.Parameter.Type.NAMED_ONLY, EMPTY_TYPE, null);
        }

        public Parameter positional() {
            return new Parameter(identifier, ParameterList.Parameter.Type.POSITIONAL_ONLY, EMPTY_TYPE, null);
        }

        public Parameter type(String ... types) {
            final var typeDeclarations = Arrays.stream(types).map(s -> new TypeDeclaration(s,null)).toArray(TypeDeclaration[]::new);
            return new Parameter(identifier, type, typeDeclarations, null);
        }

        public ParameterList.Parameter defaultExpression(String expression) {
            final var input = Input.fromString(expression, "parameter_expression");
            final var tokens = Lexer.analyze(input);
            final var command = ExpressionAnalyzer.INSTANCE.analyze(tokens);
            return new ParameterList.Parameter(identifier, type, types, command);
        }

        public ParameterList.Parameter mandatory() {
            return new ParameterList.Parameter(identifier, type, types, defaultExpression);
        }
    }
}
