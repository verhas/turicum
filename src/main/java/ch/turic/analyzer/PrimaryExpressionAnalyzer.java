package ch.turic.analyzer;


import ch.turic.BadSyntax;
import ch.turic.ExecutionException;
import ch.turic.commands.*;
import ch.turic.memory.CompositionModifier;

import java.util.ArrayList;

/**
 * <pre>
 *     {@code
 * primary_expression ::= literal
 * | 'fn' ... function definition
 * | 'class' ... class definition
 * | identifier
 * | '(' expression ')'
 * | function_call
 * | field_access
 * | method_call
 * | block_expression
 * | array_access
 * | '[' array elements ']
 *     }
 * </pre>
 */
public class PrimaryExpressionAnalyzer extends AbstractAnalyzer {
    public static final PrimaryExpressionAnalyzer INSTANCE = new PrimaryExpressionAnalyzer();
    public static final Command[] EMPTY_COMMAND_ARRAY = new Command[0];

    @Override
    public Command _analyze(LexList lexes) throws BadSyntax {
        if (lexes.isEmpty()) {
            throw new BadSyntax(lexes.position(), "Expression is empty");
        }
        if (lexes.is(Keywords.CLASS)) {
            lexes.next();
            return ClassAnalyzer.INSTANCE.analyze(lexes);
        }
        if (lexes.is(Keywords.FN)) {
            lexes.next();
            return FunctionAnalyzer.INSTANCE.analyze(lexes);
        }
        if (lexes.is("(")) {
            final var left = getExpressionBetweenParentheses(lexes);
            return getAccessOrCall(lexes, left, false);
        }
        if (lexes.is("&{")) {
            return getAccessOrCall(lexes, JsonStructureAnalyzer.INSTANCE.analyze(lexes), false);
        }
        if (lexes.is("{")) {
            if (lexes.isAt(1, "}")) {
                lexes.next();
                lexes.next();
                return getAccessOrCall(lexes, new EmptyObject(), false);
            }
            if ((lexes.isAt(1, Lex.Type.IDENTIFIER) || lexes.isAt(1, Lex.Type.STRING)) &&
                    lexes.isAt(2, ":")) {
                return getAccessOrCall(lexes, JsonStructureAnalyzer.INSTANCE.analyze(lexes), false);
            }
            return getAccessOrCall(lexes, BlockOrClosureAnalyzer.INSTANCE.analyze(lexes), false);
        }
        if (lexes.is("[")) {
            lexes.next();
            if (lexes.is("]")) {
                lexes.next();
                return getAccessOrCall(lexes, new ListComposition(EMPTY_COMMAND_ARRAY, null), false);
            }
            final var expressionList = new java.util.ArrayList<Command>();
            while (true) {
                final var expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
                expressionList.add(expression);
                if (lexes.is(",")) {
                    lexes.next();
                } else if (lexes.is("]", "?", "->")) {
                    break;
                } else {
                    throw new BadSyntax(lexes.position(), "Unexpected end of expression list in array literal");
                }
            }
            final var modifiers = getModifierChain(lexes);
            final var left = new ListComposition(expressionList.toArray(Command[]::new), modifiers);
            BadSyntax.when(lexes, lexes.isNot("]"), "list literal has to be closed using ']'");
            lexes.next();
            return getAccessOrCall(lexes, left, false);
        }
        if (lexes.is("@")) {
            lexes.next(); // step over the '@'
            ExecutionException.when(!lexes.isIdentifier(), "@ has to be followed by a function name.");
            final var lex = lexes.next();
            return getAccessOrCall(lexes, new Identifier(lex.text()), true);
        }
        final var lex = lexes.next();
        return switch (lex.type()) {
            case IDENTIFIER -> getAccessOrCall(lexes, new Identifier(lex.text()), false);
            case STRING -> getAccessOrCall(lexes, new StringConstant(lex.text()), false);
            case INTEGER -> getAccessOrCall(lexes, new IntegerConstant(lex.text()), false);
            case FLOAT -> getAccessOrCall(lexes, new FloatConstant(lex.text()), false);
            default ->
                    throw new BadSyntax(lexes.position(), "Expression: expected identifier, or constant, got '%s'", lex.text());
        }

                ;
    }

    /**
     * Get the array of filters and mappers until some stops the parsing.
     * <p>
     * The current lexical element should be the first '{@code ?}' or '{@code ->}'.
     *
     * @param lexes the lexical elements
     * @return the array of the composition modifiers
     * @throws BadSyntax if there is an error in one of the expressions following the '{@code ?}' and/or '{@code ->}'
     *                   symbols.
     */
    private static CompositionModifier[] getModifierChain(LexList lexes) throws BadSyntax {
        final var modifiers = new ArrayList<CompositionModifier>();
        while (lexes.is("?", "->")) {
            final var oper = lexes.next().text();
            final var modifierExpression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
            modifiers.add(switch (oper) {
                case "->" -> new CompositionModifier.Mapper(modifierExpression);
                case "?" -> new CompositionModifier.Filter(modifierExpression);
                default -> throw new RuntimeException("Unexpected operator: " + oper + "this is an internal error");

            });
        }
        return modifiers.toArray(CompositionModifier[]::new);
    }

    private Command getAccessOrCall(LexList lexes, Command left, boolean isDecorator) throws BadSyntax {
        while (lexes.is("(", ".", "[")) {
            left = switch (lexes.next().text()) {
                case "(" -> new FunctionCall(left, analyzeArguments(lexes, isDecorator, true));
                case "." -> new FieldAccess(left, lexes.next(Lex.Type.IDENTIFIER).text());
                case "[" -> {
                    final var indexExpression = new ArrayAccess(left, ExpressionAnalyzer.INSTANCE.analyze(lexes));
                    lexes.next(Lex.Type.RESERVED, "]", "Array indexing is not close with ]");
                    yield indexExpression;
                }
                default -> throw new IllegalStateException("Unexpected value: " + lexes.next().text());
            };
        }
        return left;
    }

    private static Command getExpressionBetweenParentheses(LexList lexes) throws BadSyntax {
        lexes.next();// step over the opening (
        final var expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
        if (lexes.is(")")) {
            lexes.next();
            return expression;
        }
        throw new BadSyntax(lexes.position(), "Expression is not well formed, missing ')'");
    }

    /**
     * Analyse the actual arguments after a function call.
     *
     * @param lexes the lexical elements positioned after the opening "("
     * @return the arguments
     * @throws BadSyntax if the syntax is bad
     */
    static FunctionCall.Argument[] analyzeArguments(LexList lexes, boolean isDecorator, boolean needsClosing) throws BadSyntax {
        final var arguments = new java.util.ArrayList<FunctionCall.Argument>();
        while (lexes.isNot(")")) {
            if (lexes.isIdentifier() && lexes.isAt(1, "=")) {
                final var id = new Identifier(lexes.next().text());
                lexes.next(); // over the '='
                final var expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
                arguments.add(new FunctionCall.Argument(id, expression));
            } else {
                final var expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
                arguments.add(new FunctionCall.Argument(null, expression));
            }
            if (lexes.isNot(",")) {
                break;
            }
            lexes.next();
        }
        if (needsClosing) {
            BadSyntax.when(lexes, lexes.isNot(")"), "Function call: expected ')' after the parents");
            lexes.next(); // consume the ')'
        }
        // the closure must start on the same line where the closing ')' was, or where the last argument finished
        if (lexes.is("{") && ClosureAnalyzer.blockStartsClosure(lexes) && !lexes.peek().atLineStart()) {
            // trailing closure works with our without setting the function call to be a decorator
            lexes.next();
            final var closure = ClosureAnalyzer.INSTANCE.analyze(lexes);
            arguments.add(new FunctionCall.Argument(null, closure));
        } else if (isDecorator) {
            if (lexes.is("fn")) {
                lexes.next();
                final var fn = FunctionAnalyzer.INSTANCE.analyze(lexes);
                arguments.add(new FunctionCall.Argument(null, fn));
            } else if (lexes.is("class")) {
                lexes.next();
                final var klass = ClassAnalyzer.INSTANCE.analyze(lexes);
                arguments.add(new FunctionCall.Argument(null, klass));
            } else {
                throw new BadSyntax(lexes.position(), "Could not find what to decorate. Only closures, functions and classes can be decorated as for now.");
            }
        }
        return arguments.toArray(FunctionCall.Argument[]::new);
    }
}
