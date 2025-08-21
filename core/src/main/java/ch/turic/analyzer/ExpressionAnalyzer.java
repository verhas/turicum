package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.commands.AsyncEvaluation;
import ch.turic.commands.AwaitEvaluation;
import ch.turic.Command;

import java.util.Map;
import java.util.Set;

/**
 * The ExpressionAnalyzer class is responsible for analyzing lexical elements and constructing the appropriate
 * command objects based on the provided syntax. This analyzer handles expressions related to asynchronous
 * execution and waiting operations, as well as delegating other expressions to the BinaryExpressionAnalyzer.
 * <p>
 * This class extends {@link AbstractAnalyzer}, inheriting the general behavior of an Analyzer while implementing
 * the specific details for expression-based commands. It operates on lexical elements provided by a {@link LexList},
 * consuming and interpreting them to create corresponding {@link Command} objects.
 * <p>
 * The ExpressionAnalyzer supports:
 * - The `async` keyword with an optional set of parameters defined in {@code ASYNC_OPTIONS}.
 * - The `await` keyword with an optional set of parameters defined in {@code AWAIT_OPTIONS}.
 * - Delegation of non-async and non-await expressions to {@link BinaryExpressionAnalyzer}.
 */
public class ExpressionAnalyzer extends AbstractAnalyzer {

    public final static Analyzer INSTANCE = new ExpressionAnalyzer();

    private static final Set<String> ASYNC_OPTIONS = Set.of("in", "out", "steps", "time");
    private static final Set<String> AWAIT_OPTIONS = Set.of("time");

    public Command _analyze(LexList lexes) throws BadSyntax {
        if (lexes.isKeyword()) {
            switch (lexes.peek().text()) {
                case Keywords.ASYNC:
                    final var asyncOptions = getOptions(lexes, "Async", ASYNC_OPTIONS);
                    return new AsyncEvaluation(analyze(lexes), asyncOptions);
                case Keywords.AWAIT:
                    final var awaitOptions = getOptions(lexes, "await", AWAIT_OPTIONS);
                    return new AwaitEvaluation(analyze(lexes), awaitOptions);
                default:
                    break;
            }
        }
        return BinaryExpressionAnalyzer.INSTANCE.analyze(lexes);
    }

    private Map<String, Command> getOptions(LexList lexes, String forWhat, Set<String> OPTIONS) {
        lexes.next();
        final Map<String, Command> asyncOptions;
        if (lexes.peek().text().equals("[")) {
            lexes.next();
            asyncOptions = OptionListAnalyzer.analyze(lexes, OPTIONS);
            BadSyntax.when(lexes, !lexes.peek().text().equals("]"), "%s options should be closed with ']'", forWhat);
            lexes.next();
        } else {
            asyncOptions = Map.of();
        }
        return asyncOptions;
    }
}
