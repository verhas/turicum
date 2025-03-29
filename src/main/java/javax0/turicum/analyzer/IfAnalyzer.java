package javax0.turicum.analyzer;


import javax0.turicum.BadSyntax;
import javax0.turicum.commands.BlockCommand;
import javax0.turicum.commands.Command;

import java.util.List;

public class IfAnalyzer implements Analyzer {
    public static final IfAnalyzer INSTANCE = new IfAnalyzer();

    public javax0.turicum.commands.If analyze(final Lex.List lexes) throws BadSyntax {
        // there is no need for '(' and ')' after the 'if'
        final var condition = ExpressionAnalyzer.INSTANCE.analyze(lexes);
        final Command thenBlock;
        if (lexes.is("{")) {
            thenBlock = BlockAnalyzer.INSTANCE.analyze(lexes);
        } else if (lexes.is(":")) {
            lexes.next();
            thenBlock = CommandAnalyzer.INSTANCE.analyze(lexes);
            BadSyntax.when( thenBlock == null ,"Empty command ( ';' ) must not be after 'if expression' or 'elseif expression'" );
        } else {
            throw new BadSyntax(": or {", "Expected '{' after if condition");
        }

        if (lexes.is(Keywords.ELSEIF)) {
            return new javax0.turicum.commands.If(condition, thenBlock, new BlockCommand(List.of(IfAnalyzer.INSTANCE.analyze(lexes)), true));
        }
        if (lexes.is(Keywords.ELSE)) {
            lexes.next();
            if (lexes.is(Keywords.IF)) { // we allow not only elseif but also 'else if'
                return new javax0.turicum.commands.If(condition, thenBlock, new BlockCommand(List.of(IfAnalyzer.INSTANCE.analyze(lexes)), true));
            }
            final Command elseBlock;
            if (lexes.is("{")) {
                elseBlock = BlockAnalyzer.INSTANCE.analyze(lexes);
            } else if (lexes.is(":")) {
                lexes.next();
                elseBlock = CommandAnalyzer.INSTANCE.analyze(lexes);
                BadSyntax.when( elseBlock == null ,"Empty command ( ';' ) must not be after 'else expression'");
            } else {
                throw new BadSyntax(": or {", "Expected '{' after if condition");
            }
            return new javax0.turicum.commands.If(condition, thenBlock, elseBlock);
        }
        return new javax0.turicum.commands.If(condition, thenBlock, null);
    }
}
