package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.Command;
import ch.turic.commands.LetAssignment;

/**
 * Represents a specialized analyzer for processing variable assignments in the form of `let` or `mut`.
 * The analyzer facilitates handling both single and multi-variable assignments with support for mutable
 * and immutable types.
 *
 * snippet EBNF_LET
 * LET ::= SINGLE_LET | MULTI_LET
 *
 * SINGLE_LET ::= ('let' | 'mut') ASSIGNMENT_LIST
 *
 * MULTI_LET ::= ('let' | 'mut') MULTI_ASSIGNMENT_HEADER '=' EXPRESSION
 *
 * MULTI_ASSIGNMENT_HEADER ::= '[' ASSIGNMENT_LIST ']'
 *                         | '{' ASSIGNMENT_LIST '}'
 *
 * ASSIGNMENT_LIST ::= ASSIGNMENT (',' ASSIGNMENT)*
 *
 * ASSIGNMENT ::= IDENTIFIER (':' TYPE_DECLARATION)?
 *
 * TYPE_DECLARATION ::= IDENTIFIER
 *
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
        if (lexes.is("[", "{")) {
            return mut? MultiLetAnalyzer.INSTANCE_MUT.analyze(lexes): MultiLetAnalyzer.INSTANCE.analyze(lexes);
        } else {
            AssignmentList.Assignment[] assignments = AssignmentList.INSTANCE.analyze(lexes);
            if (assignments.length == 0) {
                throw lexes.syntaxError("%s with zero assignments", this == INSTANCE ? "let" : "mut");
            }
            return new LetAssignment(assignments, mut);
        }
    }
}
