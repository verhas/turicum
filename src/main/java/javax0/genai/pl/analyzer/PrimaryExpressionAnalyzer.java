package javax0.genai.pl.analyzer;


import javax0.genai.pl.commands.*;

import java.util.ArrayList;

/**
 * <pre>
 *     {@code
 * primary_expression ::= literal
 * | identifier
 * | '(' expression ')'
 * | function_call
 * | field_access
 * | method_call
 * | block_expression
 * | array_access
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
        if (lexes.is("(")) {
            return getExpressionBetweenParentheses(lexes);
        }
        if (lexes.is("{")) {
            return BlockAnalyzer.INSTANCE.analyze(lexes);
        }
        final var lex = lexes.next();
        switch (lex.type) {
            case IDENTIFIER:
                Command left = new Identifier(lex.text);
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
            case STRING:
                return new StringConstant(lex.text);
            case INTEGER:
                return new IntegerConstant(lex.text);
            case FLOAT:
                return new FloatConstant(lex.text);
            default:
                throw new BadSyntax("Expression: expected identifier, or constant, got " + lex.text);
        }
    }

    private Command getExpressionBetweenParentheses(Lex.List lexes) throws BadSyntax {
        lexes.next();
        final var expression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
        if (lexes.is(")")) {
            lexes.next();
            return expression;
        }
        throw new BadSyntax("Expression is not well formed, missing ')'");
    }

    private Command[] analyzeArguments(Lex.List lexes) throws BadSyntax {
        final var arguments = new ArrayList<Command>();
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
