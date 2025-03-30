package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import javax0.turicum.commands.Command;

import java.util.HashMap;
import java.util.Map;

public class CommandAnalyzer implements Analyzer {
    public static final CommandAnalyzer INSTANCE = new CommandAnalyzer();

    @Override
    public Command analyze(Lex.List lexes) throws BadSyntax {
        if( lexes.is(";")) {
            lexes.next();
            return null;
        }
        if (lexes.isNot("{") && lexes.isKeyword()) {
            return analyzeKeywordCommand(lexes);
        }
        final var position = lexes.getIndex();
        try {
            final var assignmentCommand = AssignmentAnalyzer.INSTANCE.analyze(lexes);
            Analyzer.checkCommandTermination(lexes);
            return assignmentCommand;
        } catch (BadSyntax bs) {
            lexes.setIndex(position);
            final var expressionCommand = ExpressionAnalyzer.INSTANCE.analyze(lexes);
            Analyzer.checkCommandTermination(lexes);
            return expressionCommand;
        }
    }

    private static final Map<String, Analyzer> analyzers = new HashMap<>();



    static {
        analyzers.put(Keywords.LOCAL, LocalAnalyzer.INSTANCE);
        analyzers.put(Keywords.PIN, LocalAnalyzer.FINAL_INSTANCE);
        analyzers.put(Keywords.GLOBAL, GlobalAnalyzer.INSTANCE);
        analyzers.put(Keywords.LET, AssignmentAnalyzer.INSTANCE);
        analyzers.put(Keywords.FN, FunctionAnalyzer.INSTANCE);
        analyzers.put(Keywords.CLASS, ClassAnalyzer.INSTANCE);
        analyzers.put(Keywords.IF, IfAnalyzer.INSTANCE);
        analyzers.put(Keywords.BREAK, BreakAnalyzer.INSTANCE);
        analyzers.put(Keywords.RETURN, ReturnAnalyzer.INSTANCE);
        analyzers.put(Keywords.YIELD, YieldAnalyzer.INSTANCE);
        analyzers.put(Keywords.WHILE, WhileLoopAnalyzer.INSTANCE);
        analyzers.put(Keywords.FOR, ForLoopAnalyzer.INSTANCE);
    }

    /**
     * Analyze a command that starts with a reserved word, that is most of the commands except for
     * Assignment.
     *
     * @param lexes   the lexical elements following the keyword
     * @return the command created later used to execute
     */
    private Command analyzeKeywordCommand(final Lex.List lexes) throws BadSyntax {
        final var keyword = lexes.peek().text();
        return switch (keyword) {
            // commands that end in a block and do not need terminating ;
            case Keywords.IF,
                 Keywords.WHILE,
                 Keywords.FOR,
                 Keywords.FN,
                 Keywords.CLASS -> {
                lexes.next();
                yield analyzers.get(keyword).analyze(lexes);
            }
            // commands that may have terminating ;
            case Keywords.LOCAL,
                 Keywords.PIN,
                 Keywords.GLOBAL,
                 Keywords.BREAK,
                 Keywords.RETURN,
                 Keywords.YIELD,
                 Keywords.LET -> {
                lexes.next();// eat the keyword
                final var command = analyzers.get(keyword).analyze(lexes);
                Analyzer.checkCommandTermination(lexes);
                yield command;
            }
            default -> throw new BadSyntax("Keyword: " + keyword + " can not start a command");
        };
    }

}

