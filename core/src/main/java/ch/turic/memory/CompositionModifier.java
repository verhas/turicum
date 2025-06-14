package ch.turic.memory;

import ch.turic.Command;
import ch.turic.utils.Unmarshaller;

public class CompositionModifier {
    public static class Filter extends CompositionModifier {
        public final Command expression;

        public Filter(Command expression) {
            this.expression = expression;
        }

        public static Filter factory(final Unmarshaller.Args args) {
            return new Filter(args.command("expression"));
        }
    }

    public static class Mapper extends CompositionModifier {
        public final Command expression;

        public Mapper(Command expression) {
            this.expression = expression;
        }

        public static Mapper factory(final Unmarshaller.Args args) {
            return new Mapper(args.command("expression"));
        }
    }

    public static class Attacher extends CompositionModifier {
        public final Command expression;

        public Attacher(Command expression) {
            this.expression = expression;
        }

        public static Attacher factory(final Unmarshaller.Args args) {
            return new Attacher(args.command("expression"));
        }
    }

}
