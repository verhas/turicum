package ch.turic.commands.operators;

import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.commands.BlockCommand;
import ch.turic.commands.Conditional;
import ch.turic.commands.ListComposition;
import ch.turic.commands.StringConstant;
import ch.turic.memory.HasCommands;
import ch.turic.memory.LngList;
import ch.turic.memory.LngObject;
import ch.turic.memory.LocalContext;

@Operator.Symbol("+")
public class Add extends AbstractOperator {

    /**
     * you can write '+' in front of anything, like +"string" or even an object, that is just the same
     */
    @Override
    public Object unaryOp(LocalContext ctx, Object op) throws ExecutionException {
        return op;
    }

    /**
     * Performs addition or merging based on operand types.
     * <p>
     * If the left operand is a string, it concatenates that with the right operand as a string (disallowing control flow values). If the left operand is a list, returns a new list with elements from both operands. If both operands are objects, returns a merged object combining their fields. For all other types, performs numeric addition.
     *
     * @param op1   the left operand
     * @param right the command producing the right operand
     * @return the result of addition, concatenation, or merging, depending on operand types
     * @throws ExecutionException if string concatenation is attempted with a control flow value
     */
    @Override
    public Object binaryOp(LocalContext ctx, Object op1, Command right) throws ExecutionException {
        final var op2 = right.execute(ctx);

        // if the left side is a string, then convert it to a string
        if (op1 instanceof CharSequence s) {
            ExecutionException.when(op2 instanceof Conditional, "Cannot append break or return value to a string.");
            return s + Cast.toString(op2);
        }

        // if the left side is a block, then result in a new block command merging the two block commands
        // In this case, the right-hand side should also be a block
        if (op1 instanceof BlockCommand bq) {
            if (op2 instanceof HasCommands other) {
                if (other instanceof ListComposition olc && olc.modifiers() != null && olc.modifiers().length > 0) {
                    throw new ExecutionException("Cannot add a list composition with modifiers to a block.");
                }
                final var commandArray = new Command[bq.commands().length + other.commands().length];
                System.arraycopy(bq.commands(), 0, commandArray, 0, bq.commands().length);
                System.arraycopy(other.commands(), 0, commandArray, bq.commands().length, other.commands().length);
                return new BlockCommand(commandArray, bq.wrap());
            } else {
                throw new ExecutionException("Cannot add a %s to a block.", op2.getClass().getSimpleName());
            }
        }

        if (op1 instanceof ListComposition lc) {
            if (op2 instanceof HasCommands other) {
                if (other instanceof ListComposition olc && olc.modifiers() != null && olc.modifiers().length > 0) {
                    throw new ExecutionException("Cannot add a list composition with modifiers to a list composition.");
                }
                final var commandArray = new Command[lc.commands().length + other.commands().length];
                System.arraycopy(lc.commands(), 0, commandArray, 0, lc.commands().length);
                System.arraycopy(other.commands(), 0, commandArray, lc.commands().length, other.commands().length);
                return new ListComposition(commandArray, lc.modifiers());
            } else {
                throw new ExecutionException("Cannot add a %s to a list.", op2.getClass().getSimpleName());
            }

        }


        // if the left side is a list, then merge the lists or append the right side to the list
        if (op1 instanceof LngList list1) {
            final var joinedList = new LngList(list1.getFieldProvider());
            joinedList.array.addAll(list1.array);
            if (op2 instanceof LngList list2) {
                joinedList.array.addAll(list2.array);
            } else {
                joinedList.array.add(op2);
            }
            return joinedList;
        }

        // if the left side is an object, then merge the objects
        if (op1 instanceof LngObject obj1 && op2 instanceof LngObject obj2) {
            final var merged = new LngObject(obj1.lngClass(), ctx.open());
            for (final var f : obj1.fields()) {
                if (f.equals("this")) {
                    merged.setField(f, merged);
                } else {
                    merged.setField(f, obj1.getField(f));
                }
            }
            for (final var f : obj2.fields()) {
                if (!f.equals("cls") && !f.equals("this")) {
                    merged.setField(f, obj2.getField(f));
                }
            }
            return merged;
        }

        // you can concatenate two unevaluated string constants to get a new unevaluated string constant
        if (op1 instanceof StringConstant sc1 && op2 instanceof StringConstant sc2) {
            if (sc1.value() != null && sc2.value() != null) {
                return new StringConstant(sc1.value() + sc2.value(), false);
            }
            int l1 = sc1.value() == null ? sc1.commands().length : 1;
            int l2 = sc2.value() == null ? sc2.commands().length : 1;
            final var commands = new Command[l1 + l2];
            if (sc1.value() == null) {
                System.arraycopy(sc1.commands(), 0, commands, 0, l1);
            } else {
                commands[0] = new StringConstant(sc1.value(), false);
            }
            if (sc2.value() == null) {
                System.arraycopy(sc2.commands(), 0, commands, l1, l2);
            } else {
                commands[l1] = new StringConstant(sc2.value(), false);
            }
            return new StringConstant(null, commands);
        }

        // you can append a string (evaluated string constant) to an unevaluated string constant to get a new unevaluated string constant
        if (op1 instanceof StringConstant sc1 && op2 instanceof String string) {
            if (sc1.value() != null) {
                return new StringConstant(sc1.value() + string, false);
            }
            final var commands = new Command[sc1.commands().length + 1];
            System.arraycopy(sc1.commands(), 0, commands, 0, sc1.commands().length);
            commands[sc1.commands().length] = new StringConstant(string, false);
            return new StringConstant(null, commands);
        }

        return binary("add", op1, op2, Long::sum, Double::sum);
    }

}
