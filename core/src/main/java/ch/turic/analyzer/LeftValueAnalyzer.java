package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.memory.*;

/**
 * Analyzes left-value expressions in source code, determining what can appear
 * on the left side of an assignment operation.
 *
 * <p>A left-value (lvalue) represents a memory location that can be assigned to.
 * This analyzer recognizes and parses various forms of left-values including:
 * <ul>
 *   <li>Simple variable identifiers (e.g., {@code x})</li>
 *   <li>Object field access (e.g., {@code obj.field})</li>
 *   <li>Array element access (e.g., {@code arr[index]})</li>
 *   <li>Complex calculated expressions that evaluate to assignable locations.</li>
 * </ul>
 *
 * <p>The analyzer uses a singleton pattern for efficiency, as it maintains no
 * internal state and can be safely shared across multiple parsing operations.
 *
 * <p>Examples of valid left-values:
 * <pre>
 * variable
 * object.field
 * array[0]
 * object.array[index].field
 * {complex expression}.field[0]
 * </pre>
 */
public class LeftValueAnalyzer {
    public static final LeftValueAnalyzer INSTANCE = new LeftValueAnalyzer();

    /**
     * Analyzes a sequence of lexical tokens to determine if they form a valid left-value expression.
     *
     * <p>This method attempts to parse the beginning of the provided token list as a left-value.
     * It consumes tokens from the list as it processes them, advancing the list's position.
     * If no valid left-value can be parsed from the current position, the method returns null
     * without consuming any tokens.
     *
     * <p>The parsing process follows this general pattern:
     * <ol>
     *   <li>Identify the base left-value (variable identifier or calculated expression)</li>
     *   <li>Process any chained field accesses (.) or array indexing ([]) operations</li>
     *   <li>Return the complete left-value structure</li>
     * </ol>
     *
     * @param lexes the list of lexical tokens to analyze; tokens are consumed during parsing
     * @return a {@link LeftValue} object representing the parsed left-value expression,
     * or {@code null} if no valid left-value can be parsed from the current position, but the syntax
     * still can be okay for something else.
     * @throws BadSyntax            if the tokens form an invalid or incomplete left-value expression
     *                              (e.g., missing field name after dot operator, unclosed array access)
     */
    public LeftValue analyze(LexList lexes) throws BadSyntax {
        if (lexes.isEmpty()) {
            return null;
        }
        final var lex = lexes.peek();
        if (lex.type() == Lex.Type.IDENTIFIER) {
            LeftValue left = new VariableLeftValue(lex.text());
            lexes.next();
            return getLeftValueTail(lexes, left);
        }
        if (lex.text.equals("{") && !ClosureAnalyzer.blockStartsClosure(lexes) &&
                !((lexes.isAt(1, Lex.Type.IDENTIFIER) || lexes.isAt(1, Lex.Type.STRING)) &&
                        lexes.isAt(2, ":"))) {
            LeftValue left = new CalculatedLeftValue(BlockAnalyzer.INSTANCE.analyze(lexes));
            if (lexes.isNot(".", "[")) {
                return null;
            }
            return getLeftValueTail(lexes, left);
        }
        return null;

    }

    /**
     * Processes the "tail" portion of a left-value expression, handling chained field access
     * and array indexing operations.
     *
     * <p>This method continues parsing from a base left-value, processing any number of
     * chained operations that can appear after the initial left-value:
     * <ul>
     *   <li>Field access using dot notation (e.g., {@code .fieldName})</li>
     *   <li>Array element access using bracket notation (e.g., {@code [expression]})</li>
     * </ul>
     *
     * <p>The method processes these operations left-to-right, building a nested structure
     * of left-value objects that represent the complete access chain.
     *
     * <p>Examples of tail processing:
     * <pre>
     * .field1.field2        → ObjectFieldLeftValue wrapping ObjectFieldLeftValue
     * [0][1]               → ArrayElementLeftValue wrapping ArrayElementLeftValue
     * .field[index].other  → ObjectFieldLeftValue wrapping ArrayElementLeftValue wrapping ObjectFieldLeftValue
     * </pre>
     *
     * @param lexes the list of lexical tokens to continue parsing from
     * @param left the base left-value to which tail operations will be applied
     * @return the complete left-value expression including all processed tail operations
     * @throws BadSyntax if any tail operation is malformed (e.g., missing field name after dot,
     *                   missing closing bracket, invalid field identifier)
     */
    private LeftValue getLeftValueTail(LexList lexes, LeftValue left) {
        while (lexes.is(".") || lexes.is("[")) {
            switch (lexes.peek().text()) {
                case ".":
                    lexes.next();
                    final var fieldName = lexes.next();
                    BadSyntax.when(lexes, fieldName.type() != Lex.Type.IDENTIFIER, "Field name is invalid");
                    left = new ObjectFieldLeftValue(left, fieldName.text());
                    break;
                case "[":
                    lexes.next();
                    final var indexExpression = ExpressionAnalyzer.INSTANCE.analyze(lexes);
                    lexes.next(Lex.Type.RESERVED, "]", "Array access needs ] at the end.");
                    left = new ArrayElementLeftValue(left, indexExpression);
                    break;
            }
        }
        return left;
    }
}
