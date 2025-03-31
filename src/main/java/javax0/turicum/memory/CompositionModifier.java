package javax0.turicum.memory;

import javax0.turicum.commands.Command;

public class CompositionModifier {

    public static class Filter extends CompositionModifier {
        public final Command expression;

        public Filter(Command expression) {
            this.expression = expression;
        }
    }

    public static class Mapper extends CompositionModifier {
        public final Command expression;

        public Mapper(Command expression) {
            this.expression = expression;
        }

    }

}
