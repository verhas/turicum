package javax0.genai.pl.analyzer;

import java.util.HashMap;
import java.util.Map;

public class CommandAnalyzer implements Analyzer {
    public static final CommandAnalyzer INSTANCE = new CommandAnalyzer();

    @Override
    public javax0.genai.pl.commands.Command analyze(Lex.List lexes) throws BadSyntax {
        final var lex = lexes.peek();
        if (lex.text.equals("{")) {
            return BlockAnalyzer.INSTANCE.analyze(lexes);
        }
        if (lex.type == Lex.Type.RESERVED) {
            return analyze(lexes, lex.text);
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
        analyzers.put("local", LocalAnalyzer.INSTANCE);
        analyzers.put("final", LocalAnalyzer.FINAL_INSTANCE);
        analyzers.put("global", GlobalAnalyzer.INSTANCE);
        analyzers.put("let", AssignmentAnalyzer.INSTANCE);
        analyzers.put("fn", FunctionAnalyzer.INSTANCE);
        analyzers.put("class", ClassAnalyzer.INSTANCE);
        analyzers.put("if", IfAnalyzer.INSTANCE);
    }

    /**
     * Analyze a command that starts with a reserved word, that is most of the commands except for
     * Assignment.
     *
     * @param lexes   the lexical elements following the keyword
     * @param keyword the keyword that starts the command
     * @return the command created later used to execute
     */
    private javax0.genai.pl.commands.Command analyze(final Lex.List lexes, final String keyword) throws BadSyntax {
        return switch (keyword) {
            // commands that end in a block and do not need terminating ;
            case "if",
                 "fn",
                 "class" -> {
                lexes.next();
                yield analyzers.get(keyword).analyze(lexes);
            }
            // commands that may need terminating ;
            case "local",
                 "final",
                 "global",
                 "let" -> {
                lexes.next();// eat the keyword
                final var command = analyzers.get(keyword).analyze(lexes);
                Analyzer.checkCommandTermination(lexes);
                yield command;
            }
            default -> throw new BadSyntax("Unknown keyword: " + keyword);
        };
    }

}

