package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.commands.Command;
import ch.turic.commands.LetAssignment;
import ch.turic.commands.MLetAssignment;

public class LetAnalyzer extends AbstractAnalyzer {
    public static final LetAnalyzer INSTANCE = new LetAnalyzer();

    @Override
    public Command _analyze(LexList lexes) throws BadSyntax {
        if (lexes.is("[", "{")) {
            final var opening = lexes.next().text;
            AssignmentList.Assignment[] assignments = AssignmentList.INSTANCE.analyze(lexes, false);
            if ((opening.equals("[") && lexes.isNot("]")) || (opening.equals("{") && lexes.isNot("}"))) {
                throw lexes.syntaxError( "multi-let assignment variable list not closed");
            }
            lexes.next();
            if( lexes.isNot("=")) {
                throw lexes.syntaxError( "multi-let assignment '=' is missing");
            }
            lexes.next();
            final var rightHandSide = ExpressionAnalyzer.INSTANCE.analyze(lexes);
            return new MLetAssignment(assignments, rightHandSide,switch( opening){
                case "[" -> MLetAssignment.Type.LIST;
                case "{" -> MLetAssignment.Type.OBJECT;
                default -> throw new RuntimeException("Internal error 7343992kr6w");
            });
        } else {
            AssignmentList.Assignment[] assignments = AssignmentList.INSTANCE.analyze(lexes);
            return new LetAssignment(assignments);
        }
    }
}
