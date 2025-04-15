package ch.turic.commands.operators;

import ch.turic.commands.Command;
import ch.turic.ExecutionException;
import ch.turic.memory.Context;

import java.util.ArrayList;

@Operator.Symbol("%")
public class Mod extends AbstractOperator {

    @Override
    public Object binaryOp(Context ctx, Object op1, Command right) throws ExecutionException {
        final var op2 = right.execute(ctx);
        if( op1 instanceof String s){
            final var parameters = new ArrayList<>();
            if( op2 instanceof Iterable<?> it){
                for( Object o : it){
                    parameters.add(o);
                }
            }else{
                parameters.add(op2);
            }
            return s.formatted(parameters.toArray());
        }
        return binary("mod", op1, op2, (a, b) -> a % b, (a, b) -> a % b);
    }
}
