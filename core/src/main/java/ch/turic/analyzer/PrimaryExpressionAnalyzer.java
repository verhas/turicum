package ch.turic.analyzer;


import ch.turic.BadSyntax;
import ch.turic.Command;
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
 * | identifier | yield
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

    /**
     * Analyzes a sequence of lexical tokens to parse and construct a primary expression.
     * <p>
     * Recognizes and processes literals, identifiers, function and class definitions, yield expressions, blocks, arrays, increment/decrement operations, decorators, and various grouped or chained expressions. Delegates to specialized analyzers for complex constructs and throws a syntax error if the input is empty or contains unexpected tokens.
     *
     * @param lexes the list of lexical tokens representing the expression to analyze
     * @return a {@link Command} representing the parsed primary expression
     * @throws BadSyntax if the expression is empty or contains invalid syntax
     */
    @Override
    public Command _analyze(LexList lexes) throws BadSyntax {
        if (lexes.isEmpty()) {
            throw lexes.syntaxError("Expression is empty");
        }
        if (lexes.is(Keywords.YIELD)) {
            lexes.next();
            // eat optional '(' and ')' if there is any
            if (lexes.is("(")) {
                lexes.next();
                if (lexes.isNot(")")) {
                    throw lexes.syntaxError("Expected a closing parenthesis after 'yield'");
                }
                lexes.next();
            }
            return new YieldFetch();
        }
        if (lexes.is(Keywords.ASYNC)) {
            return ExpressionAnalyzer.INSTANCE.analyze(lexes);
        }
        if (lexes.is(Keywords.CLASS)) {
            lexes.next();
            return ClassAnalyzer.INSTANCE.analyze(lexes);
        }
        if (lexes.is(Keywords.IF)) {
            lexes.next();
            final var ifExpression = IfAnalyzer.INSTANCE.analyze(lexes);
            final int index = lexes.getIndex()-1;
            if( lexes.lexAt(index).is(";") ){
                lexes.setIndex(index);
            }
            return ifExpression;
        }
        if (lexes.is(Keywords.FN)) {
            lexes.next();
            return FunctionDefinitionAnalyzer.INSTANCE.analyze(lexes);
        }
        if (lexes.is("(")) {
            final var left = BlockAnalyzer.FLAT.analyze(lexes);
            return getAccessOrCall(lexes, left, false);
        }
        if (lexes.is("&{")) {
            return getAccessOrCall(lexes, JsonStructureAnalyzer.INSTANCE.analyze(lexes), false);
        }
        if ((lexes.is("++", "--") && lexes.isAt(1, Lex.Type.IDENTIFIER) && !lexes.isAt(2, "[", ".", "(")) ||
                (lexes.isIdentifier() && lexes.isAt(1, "++", "--"))) {
            return AssignmentAnalyzer.INSTANCE.analyze(lexes);
        }
        if (lexes.is("{")) {
            if (lexes.isAt(1, "}")) {
                lexes.next();
                lexes.next();
                return getAccessOrCall(lexes, EmptyObject.INSTANCE, false);
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
                } else if (lexes.is("]", "?", "->", "with")) {
                    break;
                } else {
                    throw lexes.syntaxError("Unexpected end of expression list in array literal");
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
        try{
        return switch (lex.type()) {
            case IDENTIFIER -> getAccessOrCall(lexes, new Identifier(lex.text()).fixPosition(lex), false);
            case STRING -> getAccessOrCall(lexes, new StringConstant(lex.text, lex.interpolated).fixPosition(lex), false);
            case INTEGER -> getAccessOrCall(lexes, new IntegerConstant(lex.text()).fixPosition(lex), false);
            case FLOAT -> getAccessOrCall(lexes, new FloatConstant(lex.text()).fixPosition(lex), false);
            default -> throw lexes.syntaxError("Expression: expected identifier, or constant, got '%s'", lex.text());
        };}catch(ExecutionException ee){
            throw lexes.syntaxError(ee.getMessage());
        }
    }

    /**
     * Get the array of filters and mappers until one stops the parsing.
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
        boolean attached = false;
        while (lexes.is("?", "->", "with")) {
            if (attached) {
                throw lexes.syntaxError("No filers and modifiers are allowed after 'with'");
            }
            final var oper = lexes.next().text();
            final var modifierExpression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
            modifiers.add(switch (oper) {
                case "->" -> new CompositionModifier.Mapper(modifierExpression);
                case "?" -> new CompositionModifier.Filter(modifierExpression);
                case "with" -> {
                    attached = true;
                    yield new CompositionModifier.Attacher(modifierExpression);
                }
                default -> throw new RuntimeException("Unexpected operator: " + oper + "this is an internal error");

            });
        }
        return modifiers.toArray(CompositionModifier[]::new);
    }

    /**
     * Handles access or function calls on a base expression, iterating over tokens like
     * parentheses, dots, optional chaining, or array indexing to chain accesses or calls
     * until no further valid tokens remain.
     *
     * @param lexes       the list of lexical tokens to analyze, positioned at or after the base expression
     * @param left        the base expression to which access or function calls will be applied
     * @param isDecorator a flag indicating whether the context of parsing involves a decorator
     * @return a {@code Command} representing the parsed access or function call expression
     * @throws BadSyntax if the token sequence contains syntax errors or unexpected tokens
     */
    private Command getAccessOrCall(LexList lexes, Command left, boolean isDecorator) throws BadSyntax {
        while (lexes.is("(", ".", "?.", "[", ".(")) {
            left = switch (lexes.next().text()) {
                case ".(" -> new FunctionCurrying(left, analyzeArguments(lexes, isDecorator, true));
                case "(" -> new FunctionCall(left, analyzeArguments(lexes, isDecorator, true));
                case "." -> new FieldAccess(left, lexes.next(Lex.Type.IDENTIFIER).text(), false);
                case "?." -> new FieldAccess(left, lexes.next(Lex.Type.IDENTIFIER).text(), true);
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
                final var fn = FunctionDefinitionAnalyzer.INSTANCE.analyze(lexes);
                arguments.add(new FunctionCall.Argument(null, fn));
            } else if (lexes.is("class")) {
                lexes.next();
                final var klass = ClassAnalyzer.INSTANCE.analyze(lexes);
                arguments.add(new FunctionCall.Argument(null, klass));
            } else {
                throw lexes.syntaxError("Could not find what to decorate. Only closures, functions and classes can be decorated as for now.");
            }
        }
        return arguments.toArray(FunctionCall.Argument[]::new);
    }
}
