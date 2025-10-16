package ch.turic.analyzer;

import ch.turic.exceptions.BadSyntax;
import ch.turic.exceptions.ExecutionException;
import ch.turic.Command;
import ch.turic.commands.FlowCommand;

import java.util.ArrayList;

public class FlowAnalyzer extends AbstractAnalyzer {
    public static final FlowAnalyzer INSTANCE = new FlowAnalyzer();
    private static final String LIMIT = "limit";
    private static final String TIMEOUT = "timeout";

    @Override
    public Command _analyze(LexList lexes) throws BadSyntax {
        final var flowStartPos = lexes.startPosition();
        Command exitCondition = null;
        Command limitExpression = null;
        Command timeoutExpression = null;
        final String flowId;
        final var cellIds = new ArrayList<String>();
        final var cells = new ArrayList<Command>();
        if (lexes.isIdentifier() && !lexes.isIdentifier(LIMIT, TIMEOUT)) {
            flowId = lexes.next().text;
        } else {
            flowId = null;
        }
        while (lexes.isNot("{")) {
            if (lexes.is(Keywords.UNTIL)) {
                if (exitCondition != null) {
                    throw lexes.syntaxError("There can only be one exit condition in flow.");
                }
                lexes.next();
                exitCondition = CommandAnalyzer.INSTANCE.analyze(lexes);
                if (lexes.is(";")) {
                    lexes.next();
                }
            }
            if (lexes.isIdentifier(LIMIT)) {
                if (limitExpression != null) {
                    throw lexes.syntaxError("There can only be one limit in flow.");
                }
                lexes.next();
                limitExpression = CommandAnalyzer.INSTANCE.analyze(lexes);
                if (lexes.is(";")) {
                    lexes.next();
                }
            }
            if (lexes.isIdentifier(TIMEOUT)) {
                if (timeoutExpression != null) {
                    throw lexes.syntaxError("There can only be one timeout in flow.");
                }
                lexes.next();
                timeoutExpression = CommandAnalyzer.INSTANCE.analyze(lexes);
            }
        }
        Command resultExpression = null;
        if (lexes.is("{")) {
            lexes.next();
            while (lexes.isNot("}")) {
                if (lexes.is(Keywords.YIELD)) {
                    lexes.next();
                    resultExpression = CommandAnalyzer.INSTANCE.analyze(lexes);
                    // it has to be the last command
                    break;
                }
                if (lexes.isIdentifier()) {
                    final String cellIdentifier = lexes.next().text();
                    if (lexes.is("<-")) {
                        lexes.next();
                        final var cellCommand = CommandAnalyzer.INSTANCE.analyze(lexes);
                        cellIds.add(cellIdentifier);
                        cells.add(cellCommand);
                    } else {
                        throw lexes.syntaxError("in flow 'x <- expression' statements are expected");
                    }
                } else {
                    throw lexes.syntaxError("Expected identifier or yield in 'flow' command");
                }
            }
            if (lexes.isNot("}")) {
                throw lexes.syntaxError("Flow command closing '}' is missing");
            }
            lexes.next();
            try {
                return new FlowCommand(flowId,exitCondition, limitExpression, timeoutExpression, resultExpression, cellIds.toArray(String[]::new), cells.toArray(Command[]::new));
            } catch (ExecutionException e) {
                final var bs = new BadSyntax(flowStartPos, e.getMessage());
                bs.setStackTrace(e.getStackTrace());
                throw bs;
            }
        } else {
            if (lexes.is(":")) {
                throw lexes.syntaxError("The flow command does not allow ': command' ");
            }
            throw lexes.syntaxError("in flow 'x expression' statements are expected");
        }
    }
}
