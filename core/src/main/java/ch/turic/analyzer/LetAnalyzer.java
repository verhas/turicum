package ch.turic.analyzer;

import ch.turic.Command;
import ch.turic.commands.LetAssignment;
import ch.turic.exceptions.BadSyntax;

/**
 * Represents a specialized analyzer for processing variable assignments in the form of `let` or `mut`.
 * The analyzer facilitates handling both single and multi-variable assignments with support for mutable
 * and immutable types.
 * <p>
 * snippet EBNF_LET
 * LET ::= SINGLE_LET | MULTI_LET
 * <p>
 * SINGLE_LET ::= ('let' | 'mut') ASSIGNMENT_LIST
 * <p>
 * MULTI_LET ::= ('let' | 'mut') DESTRUCTURING '=' EXPRESSION
 * <p>
 * DESTRUCTURING ::= '[' MAP_LIST ']'
 * | '{' FIELD_MAP_LIST '}'
 * <p>
 * MAP_LIST ::= ['let'] MAP ( ',' ['let'] MAP )*
 * <p>
 * FIELD_MAP_LIST ::== ['let'] FIELD_MAP (',' ['let'] FIELD_MAP)*
 * <p>
 * MAP ::_= IDENTIFIER [':' TYPE_DECLARATIONS ] | LEFT_VALUE | DESTRUCTURING
 * <p>
 * FIELD_MAP ::= [ IDENTIFIER '->' ] MAP
 * <p>
 * TYPE_DECLARATIONS ::= TYPE_DECLARATION ( '|' TYPE_DECLARATION )*
 * <p>
 * TYPE_DECLARATION ::= IDENTIFIER | '(' EXPRESION ')'
 * <p>
 * EXPRESSION ::= // Any valid Turicum expression
 * end snippet
 *
 */
public class LetAnalyzer extends AbstractAnalyzer {
    public static final LetAnalyzer INSTANCE = new LetAnalyzer(false);
    public static final LetAnalyzer INSTANCE_MUT = new LetAnalyzer(true);

    private final boolean mut;

    public LetAnalyzer(boolean mut) {
        this.mut = mut;
    }

    @Override
    public Command _analyze(LexList lexes) throws BadSyntax {
        if (!mut && lexes.is("mut")) {
            lexes.next();
            return INSTANCE_MUT.analyze(lexes);
        }
        if (lexes.is("[", "{")) {
            return mut ? MultiLetAnalyzer.INSTANCE_MUT.analyze(lexes) : MultiLetAnalyzer.INSTANCE.analyze(lexes);
        } else {
            AssignmentList.Assignment[] assignments = AssignmentList.INSTANCE.analyze(lexes);
            if (assignments.length == 0) {
                throw lexes.syntaxError("%s with zero assignments", this == INSTANCE ? "let" : "mut");
            }
            return new LetAssignment(assignments, mut);
        }
    }
}
