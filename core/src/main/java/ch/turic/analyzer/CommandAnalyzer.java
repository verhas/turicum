package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.commands.Command;
import ch.turic.commands.FunctionCall;
import ch.turic.commands.Identifier;

import java.util.HashMap;
import java.util.Map;

import static ch.turic.analyzer.PrimaryExpressionAnalyzer.analyzeArguments;

public class CommandAnalyzer extends AbstractAnalyzer {
    public static final CommandAnalyzer INSTANCE = new CommandAnalyzer();

    @Override
    public Command _analyze(LexList lexes) throws BadSyntax {
        if (lexes.is(";")) {
            lexes.next();
            return null;
        }
        if (lexes.isNot("{") && lexes.isKeyword()) {
            final var command =  analyzeKeywordCommand(lexes);
            if( command != null ) {
                return command;
            }
        }
        Command assignmentCommand = tryToGetAssignment(lexes);
        if (assignmentCommand != null) {
            return assignmentCommand;
        }
        FunctionCall functionCallCommand = getParentheseslessFunctionCall(lexes);
        if (functionCallCommand != null) {
            return functionCallCommand;
        }
        final var expressionCommand = ExpressionAnalyzer.INSTANCE.analyze(lexes);
        Analyzer.checkCommandTermination(lexes);
        return expressionCommand;

    }

    /**
     * Analyze a function call that is a whole command and as such does not use '(' and ')' around the arguments.
     *
     * @param lexes the input tokens
     * @return the analyzed command or null
     */
    private FunctionCall getParentheseslessFunctionCall(LexList lexes) {
        final var position = lexes.getIndex();
        if (lexes.isIdentifier()) {
            final var functionName = lexes.next().text();
            if (!lexes.hasNext() || lexes.peek().atLineStart()) {
                lexes.setIndex(position);
                return null;
            }
            if (lexes.isIdentifier() || lexes.isConstant() || lexes.is("{")) {
                final var arguments = analyzeArguments(lexes, false, false);
                return new FunctionCall(new Identifier(functionName), arguments);
            }
        }
        lexes.setIndex(position);
        return null;
    }

    /**
     * Save the lexical position and try to analyze the input tokens as an assignment. When it fails, reset the lexical
     * position and return null. When successful, return the assignment command.
     *
     * @param lexes the token list
     * @return the result of the analysis or {@code null} if it failed.
     */
    private Command tryToGetAssignment(LexList lexes) {
        final var position = lexes.getIndex();
        final var assignmentCommand = AssignmentAnalyzer.INSTANCE.analyze(lexes);
        if (assignmentCommand != null) {
            Analyzer.checkCommandTermination(lexes);
            return assignmentCommand;
        }
        lexes.setIndex(position);
        return null;
    }

    private static final Map<String, Analyzer> analyzers = new HashMap<>();


    static {
        analyzers.put(Keywords.LET, LetAnalyzer.INSTANCE);
        analyzers.put(Keywords.PIN, PinAnalyzer.INSTANCE);
        analyzers.put(Keywords.GLOBAL, GlobalAnalyzer.INSTANCE);
        analyzers.put(Keywords.FN, FunctionDefinitionAnalyzer.INSTANCE);
        analyzers.put(Keywords.CLASS, ClassAnalyzer.INSTANCE);
        analyzers.put(Keywords.IF, IfAnalyzer.INSTANCE);
        analyzers.put(Keywords.TRY, TryCatchAnalyzer.INSTANCE);
        analyzers.put(Keywords.BREAK, BreakAnalyzer.INSTANCE);
        analyzers.put(Keywords.RETURN, ReturnAnalyzer.INSTANCE);
        analyzers.put(Keywords.YIELD, YieldAnalyzer.INSTANCE);
        analyzers.put(Keywords.WHILE, WhileLoopAnalyzer.INSTANCE);
        analyzers.put(Keywords.WITH, WithAnalyzer.INSTANCE);
        analyzers.put(Keywords.FOR, ForLoopAnalyzer.INSTANCE);
        analyzers.put(Keywords.FLOW, FlowAnalyzer.INSTANCE);
    }

    /**
     * Analyze a command that starts with a reserved word, that is most of the commands except for
     * Assignment.
     *
     * @param lexes the lexical elements following the keyword
     * @return the command created later used to execute
     */
    private Command analyzeKeywordCommand(final LexList lexes) throws BadSyntax {
        final var keyword = lexes.peek().text();
        return switch (keyword) {
            // commands that end in a block and do not need terminating ;
            case Keywords.IF,
                 Keywords.WHILE,
                 Keywords.WITH,
                 Keywords.FOR,
                 Keywords.FLOW,
                 Keywords.FN,
                 Keywords.CLASS -> {
                lexes.next();
                yield analyzers.get(keyword).analyze(lexes);
            }
            // commands that may have terminating ;
            case Keywords.PIN,
                 Keywords.GLOBAL,
                 Keywords.BREAK,
                 Keywords.RETURN,
                 Keywords.YIELD,
                 Keywords.TRY,
                 Keywords.LET -> {
                lexes.next();// eat the keyword
                final var command = analyzers.get(keyword).analyze(lexes);
                Analyzer.checkCommandTermination(lexes);
                yield command;
            }
            default -> null;
        };
    }

}

