package ch.turic.analyzer;

import ch.turic.Command;
import ch.turic.commands.ConstantExpression;
import ch.turic.exceptions.BadSyntax;

/**
 * Utility class for analyzing and extracting loop body syntax in Turicum.
 *
 * <p>This class provides helper methods for the loop analysis phase of the Turicum
 * compiler/interpreter. It handles the parsing and interpretation of a loop body
 * construct, which may be expressed using either compact syntax (single colon)
 * or block syntax (curly braces).</p>
 *
 * <p>The class is designed as a utility with static methods and should not be
 * instantiated.</p>
 *
 */
public class LoopAnalyzerUtils {
    /**
     * Extracts and analyzes the body of a loop construct.
     *
     * <p>This method parses the loop body content following a loop declaration.
     * It supports two syntactic forms:</p>
     *
     * <ul>
     *   <li><strong>Compact form:</strong> Single statement prefixed with ':'
     *       (e.g., {@code for i in range: print(i)})</li>
     *   <li><strong>Block form:</strong> Multiple statements enclosed in curly braces
     *       (e.g., {@code for i in range { print(i); x++ }})</li>
     * </ul>
     *
     * <p><strong>Note:</strong> The caller is responsible for ensuring that the
     * current lexical token represents either a ':' or '{' character. This method
     * assumes that initial syntax validation has been performed.</p>
     *
     * @param lexes the current lexical sequence to analyze. The lexical pointer
     *              should be positioned at the token immediately preceding the
     *              loop body start marker (':' or '{')
     *
     * @return a {@link Command} object representing the parsed loop body.
     *         For compact syntax, returns a single {@code Command} instance.
     *         For block syntax, returns a {@code Command} that encapsulates
     *         the entire block structure.
     *
     * @throws BadSyntax if the loop body does not begin with ':' or '{',
     *                    or if any underlying analysis phase encounters a
     *                    syntax error during command or block parsing
     *
     * @see WhileLoopAnalyzer
     * @see ForLoopAnalyzer
     * @see ch.turic.exceptions.BadSyntax
     */
    static Command getLoopBody(LexList lexes) throws BadSyntax {
        Command body;
        if (lexes.is(":")) {
            lexes.next();
            body = CommandAnalyzer.INSTANCE.analyze(lexes);
        } else if (lexes.is("{")) {
            body = BlockAnalyzer.INSTANCE.analyze(lexes);
        } else {
            throw lexes.syntaxError("Loop body must start with ':' or '{'.");
        }
        return body;
    }

    /**
     * Extracts and analyzes an optional loop exit condition.
     *
     * <p>This method parses an optional exit condition that may follow a loop body.
     * Exit conditions are expressed using the {@code until} keyword followed by an
     * expression that evaluates to a boolean value. If no exit condition is specified,
     * the method returns a default constant expression representing {@code false},
     * indicating that the loop will not exit early based on a condition.</p>
     *
     * <p><strong>Syntax:</strong></p>
     * <ul>
     *   <li>{@code until <expression>} - Loop exits when the expression evaluates to true</li>
     *   <li><em>absent</em> - Loop continues normally with no conditional early exit</li>
     * </ul>
     *
     * <p><strong>Example usage:</strong></p>
     * <pre>
     *   while true: print(x) until x > 10
     * </pre>
     *
     * @param lexes the current lexical sequence to analyze. The lexical pointer
     *              should be positioned at the first token after the loop body,
     *              where the optional {@code until} keyword may appear
     *
     * @return a {@link Command} object representing the exit condition.
     *         If the {@code until} keyword is present, returns a {@code Command}
     *         representing the parsed boolean expression.
     *         If the {@code until} keyword is absent, returns a {@link ConstantExpression}
     *         with value {@code false}, indicating no early exit condition.
     *
     * @throws BadSyntax if the {@code until} keyword is present but the following
     *                    expression cannot be properly analyzed, or if the command
     *                    termination is not properly established after the condition
     *
     * @see Keywords#UNTIL
     * @see ch.turic.exceptions.BadSyntax
     */
    static Command getOptionalExistCondition(LexList lexes) throws BadSyntax {
        final Command exitCondition;
        if (lexes.is(Keywords.UNTIL)) {
            lexes.next();
            exitCondition = ExpressionAnalyzer.INSTANCE.analyze(lexes);
            Analyzer.checkCommandTermination(lexes);
        } else {
            exitCondition = new ConstantExpression(false);
        }
        return exitCondition;
    }
}
