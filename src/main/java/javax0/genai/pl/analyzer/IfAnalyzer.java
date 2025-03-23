package javax0.genai.pl.analyzer;


import javax0.genai.pl.commands.BlockCommand;

import java.util.List;

public class IfAnalyzer implements Analyzer {
    public static final IfAnalyzer INSTANCE = new IfAnalyzer();

    public javax0.genai.pl.commands.If analyze(final Lex.List lexes) throws BadSyntax {
        // there is no need for '(' and ')' after the 'if'
        final var condition = ExpressionAnalyzer.INSTANCE.analyze(lexes);
        lexes.peek(Lex.Type.RESERVED, "{", "Expected '{' after if condition");
        final var thenBlock = BlockAnalyzer.INSTANCE.analyze(lexes);

        if (lexes.is("elseif")) {
            return new javax0.genai.pl.commands.If(condition, thenBlock, new BlockCommand(List.of(IfAnalyzer.INSTANCE.analyze(lexes)),true));
        }
        if (lexes.is("else")) {
            lexes.next();
            if (lexes.is("if")) { // we allow not only elseif but also 'else if'
                return new javax0.genai.pl.commands.If(condition, thenBlock, new BlockCommand(List.of(IfAnalyzer.INSTANCE.analyze(lexes)),true));
            }
            final var otherwise = BlockAnalyzer.INSTANCE.analyze(lexes);
            return new javax0.genai.pl.commands.If(condition, thenBlock, otherwise);
        }
        return new javax0.genai.pl.commands.If(condition, thenBlock, null);
    }
}
