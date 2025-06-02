package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.commands.AsyncEvaluation;
import ch.turic.commands.AwaitEvaluation;
import ch.turic.Command;

import java.util.Map;
import java.util.Set;

/**
 * <pre>{@code
 * expression ::= binary_expression[0]
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
