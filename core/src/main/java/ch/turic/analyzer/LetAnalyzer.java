package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.Command;
import ch.turic.commands.LetAssignment;
import ch.turic.commands.MultiLetAssignment;

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
            final var opening = lexes.next().text;
            AssignmentList.Assignment[] assignments = AssignmentList.INSTANCE.analyze(lexes, false);
            if ((opening.equals("[") && lexes.isNot("]")) || (opening.equals("{") && lexes.isNot("}"))) {
                throw lexes.syntaxError("multi-let assignment variable list not closed");
            }
            lexes.next();
            if (lexes.isNot("=")) {
                throw lexes.syntaxError("multi-let assignment '=' is missing");
            }
            lexes.next();
            final var rightHandSide = ExpressionAnalyzer.INSTANCE.analyze(lexes);
            return new MultiLetAssignment(assignments, rightHandSide, switch (opening) {
                case "[" -> MultiLetAssignment.Type.LIST;
                case "{" -> MultiLetAssignment.Type.OBJECT;
                default -> throw new RuntimeException("Internal error 7343992kr6w");
            }, mut);
        } else {
            AssignmentList.Assignment[] assignments = AssignmentList.INSTANCE.analyze(lexes);
            if (assignments.length == 0) {
                throw lexes.syntaxError("%s with zero assignments", this == INSTANCE ? "let" : "mut");
            }
            return new LetAssignment(assignments, mut);
        }
    }
}
