package javax0.turicum.analyzer;


import javax0.turicum.commands.*;

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
public class PrimaryExpressionAnalyzer implements Analyzer {
    public static final PrimaryExpressionAnalyzer INSTANCE = new PrimaryExpressionAnalyzer();

    @Override
    public Command analyze(Lex.List lexes) throws BadSyntax {
        if (lexes.isEmpty()) {
            throw new BadSyntax("Expression is empty");
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
            return getAccessOrCall(lexes, left);
        }
        if (lexes.is("{")) {
            final var left = BlockOrClosureAnalyser.INSTANCE.analyze(lexes);
            return getAccessOrCall(lexes, left);
        }
        if (lexes.is("[")) {
            lexes.next();
            final var expressionList = new java.util.ArrayList<Command>();
            while (true) {
                final var expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
                expressionList.add(expression);
                if (lexes.is(",")) {
                    lexes.next();
                } else if (lexes.is("]")) {
                    lexes.next();
                    break;
                } else {
                    throw new BadSyntax("Unexpected end of expression list in array literal");
                }
            }
            final var left = new ListComposition(expressionList.toArray(Command[]::new));
            return getAccessOrCall(lexes, left);
        }
        final var lex = lexes.next();
        return switch (lex.type) {
            case IDENTIFIER -> getAccessOrCall(lexes, new Identifier(lex.text));
            case STRING -> new StringConstant(lex.text);
            case INTEGER -> new IntegerConstant(lex.text);
            case FLOAT -> new FloatConstant(lex.text);
            default -> throw new BadSyntax("Expression: expected identifier, or constant, got " + lex.text);
        }

                ;
    }

    private Command getAccessOrCall(Lex.List lexes, Command left) throws BadSyntax {
        while (lexes.is("(", ".", "[")) {
            left = switch (lexes.next().text) {
                case "(" -> new FunctionCall(left, analyzeArguments(lexes));
                case "." -> new FieldAccess(left, lexes.next(Lex.Type.IDENTIFIER).text);
                case "[" -> {
                    final var indexExpression = new ArrayAccess(left, ExpressionAnalyzer.INSTANCE.analyze(lexes));
                    lexes.next(Lex.Type.RESERVED, "]", "Array indexing is not close with ]");
                    yield indexExpression;
                }
                default -> throw new IllegalStateException("Unexpected value: " + lexes.next().text);
            };
        }
        return left;
    }

    private static Command getExpressionBetweenParentheses(Lex.List lexes) throws BadSyntax {
        lexes.next();
        final var expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
        if (lexes.is(")")) {
            lexes.next();
            return expression;
        }
        throw new BadSyntax("Expression is not well formed, missing ')'");
    }

    private static Command[] analyzeArguments(Lex.List lexes) throws BadSyntax {
        final var arguments = new java.util.ArrayList<Command>();
        while (lexes.isNot(")")) {
            arguments.add(ExpressionAnalyzer.INSTANCE.analyze(lexes));
            if (lexes.is(",")) {
                lexes.next();
            }
        }
        BadSyntax.when(lexes.isNot(")"), "Function call: expected ')' after the parents");
        lexes.next(); // consume the ')'
        return arguments.toArray(Command[]::new);
    }
}
