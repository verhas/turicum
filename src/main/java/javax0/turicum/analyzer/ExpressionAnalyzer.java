package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import javax0.turicum.commands.Command;

/**
 * <pre>{@code
 * expression ::= binary_expression[0]
 * // Define precedence levels using indexed rules
 * binary_expression[i <N ] ::= binary_expression[i+1] { binary_operators[i] binary_expression[i+1] }
 * binary_expression[i == N] ::= unary_expression  // Highest precedence level
 *
 * // Unary operators and primary expressions
 * unary_expression ::= prefix_unary_operator unary_expression | primary_expression
 *
 * primary_expression ::= literal
 * | fn ... function definition
 * | identifier
 * | '(' expression ')'
 * | function_call
 * | field_access
 * | method_call
 * | block_expression
 * | array_access
 *
 * // Function call: name(params)
 * function_call ::= identifier '(' [ expression { ',' expression } ] ')'
 *
 * // Field access: obj.field
 * field_access ::= primary_expression '.' identifier
 *
 * // Method call: obj.method(params)
 * method_call ::= primary_expression '.' identifier '(' [ expression { ',' expression } ] ')'
 *
 * // Block returning an expression value
 * block_expression ::= '{' statement { statement } expression '}'
 *
 * // Array element access: array[index]
 * array_access ::= primary_expression '[' expression ']'
 *
 * // Unary operators
 * prefix_unary_operator ::= '+' | '-' | '!'
 *
 * // Binary operators grouped by precedence (lower index = higher precedence)
 * binary_operators[0] ::= '||'   // Lowest precedence
 * binary_operators[1] ::= '&&'
 * binary_operators[2] ::= '|'
 * binary_operators[3] ::= '^'
 * binary_operators[4] ::= '&'
 * binary_operators[5] ::= '==' | '!='
 * binary_operators[6] ::= '<' | '<=' | '>' | '>='
 * binary_operators[7] ::= '<<' | '>>'
 * binary_operators[8] ::= '+' | '-'
 * binary_operators[9] ::= '*' | '/' | '%'  // Highest precedence for binary ops
 * }
 * </pre>
 *
 */

public class ExpressionAnalyzer implements Analyzer {
    public final static Analyzer INSTANCE = new ExpressionAnalyzer();

    public Command analyze(final LexList lexes) throws BadSyntax {
        return BinaryExpressionAnalyzer.INSTANCE.analyze(lexes);
    }
}
