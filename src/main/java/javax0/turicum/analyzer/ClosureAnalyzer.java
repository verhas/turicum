package javax0.turicum.analyzer;

import javax0.turicum.BadSyntax;
import javax0.turicum.commands.BlockCommand;
import javax0.turicum.commands.ClosureDefinition;
import javax0.turicum.commands.Command;
import javax0.turicum.commands.ParameterList;

/**
 * <pre>{@code
 * { |a,b,c,d| commands }
 * }</pre>
 */
public class ClosureAnalyzer implements Analyzer {
    public static final ClosureAnalyzer INSTANCE = new ClosureAnalyzer();

    /**
     * Analyze a closure.
     * <p>
     * When this method is invoked, then the syntax analyzer already knows that what we see is a closure and not a
     * block. It analyzes the code and returns a command that, when evaluated, will return a closure.
     *
     * @param lexes the lexical elements. The current lexical element has to be the opening '{'
     * @return the command resulting in a closure
     * @throws BadSyntax obviously, when there is a syntax error
     */
    @Override
    public Command analyze(Lex.List lexes) throws BadSyntax {
        final var opening = lexes.next();
        final ParameterList identifiers;
        if (opening.is("|")) {// if it is '||' then there are no identifiers
            identifiers = ParameterDefinition.INSTANCE.analyze(lexes);
            lexes.peek(Lex.Type.RESERVED, "|", "Closure arguments but be between two '|' characters");
            lexes.next();
        } else {
            identifiers = ParameterList.EMPTY;
        }
        final var commands = BlockAnalyzer.getCommands(lexes);
        final var block = new BlockCommand(commands, true);
        return new ClosureDefinition(identifiers, block);
    }

    /**
     * A closure starts with a {@code |a,b,c|} parameter list, thus when the first lexical element after the opening
     * '{' is a '{@code |} then it is a closure and not a command block.
     * <p>
     * However, when the parameter list is empty, and the programmer does not leave space between the '{@code |}'
     * characters then the lexical analyzer will present it as '{@code ||}'.
     *
     * @param lexes the lexical elements. The current lexical element has to be the opening '{'
     * @return {@code true} if this block is the start of a closure
     */
    public static boolean blockStartsClosure(Lex.List lexes) {
        return lexes.isAt(1, "|", "||");
    }

}
