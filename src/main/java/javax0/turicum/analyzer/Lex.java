package javax0.turicum.analyzer;


import javax0.turicum.BadSyntax;

public record Lex(Type type, String text, boolean atLineStart) {

    @Override
    public String toString() {
        return String.format("Lex{type=%s, text='%s'}", type, text);
    }

    public enum Type {
        IDENTIFIER, RESERVED, STRING, INTEGER, FLOAT
    }

    public boolean is(String... textAlternatives) {
        if (type != Type.RESERVED) {
            return false;
        }
        for (final var s : textAlternatives)
            if (this.text().equals(s)) {
                return true;
            }
        return false;
    }

    public static class List {
        private final Lex[] lexes;

        /**
         * Get the current index. It can be saved and in the case a syntax analysis fails that is not implemented using
         * single token look ahead then {@link #setIndex(int)} can be used to restore the lexer state.
         *
         * @return the current index
         */
        public int getIndex() {
            return index;
        }

        /**
         * Set the current index. Must not be used other than setting the index to a saved value that was returned by
         * {@link #getIndex()}.
         *
         * @param index the index where we want to reset the lexical analysis
         */
        public void setIndex(int index) {
            this.index = index;
        }

        private int index;

        public List(final java.util.List<Lex> lexes) {
            this.lexes = lexes.toArray(Lex[]::new);
        }

        public Lex next() throws BadSyntax {
            if (index >= lexes.length) {
                throw new BadSyntax("more elements expected");
            }
            return lexes[index++];
        }

        public Lex next(Lex.Type expectedType) throws BadSyntax {
            final var lex = next();
            BadSyntax.when(lex.type()!= expectedType, "%s was expected and got '%s' which is %s", expectedType.name(), lex.text(), lex.type.name());
            return lex;
        }

        public Lex next(Type type, String msg) throws BadSyntax {
            if (index >= lexes.length || lexes[index].type()!= type) {
                throw new BadSyntax(msg);
            }
            return next();
        }

        public Lex next(Type type, String text, String msg) throws BadSyntax {
            if (index >= lexes.length || lexes[index].type()!= type || !lexes[index].text().equals(text)) {
                throw new BadSyntax(msg);
            }
            return next();
        }

        public void peek(Type type, String text, String msg) throws BadSyntax {
            if (index >= lexes.length || lexes[index].type()!= type || (text != null && !text.equals(lexes[index].text()))) {
                throw new BadSyntax(msg);
            }
        }

        public void next(String text, String msg) throws BadSyntax {
            next(Type.RESERVED, text, msg);
        }

        public Lex peek() throws BadSyntax {
            if (index >= lexes.length) {
                throw new BadSyntax("more elements expected");
            }
            return lexes[index];
        }

        public boolean hasNext() {
            return index < lexes.length;
        }

        public boolean hasNext(int i) {
            return index + i < lexes.length;
        }

        public boolean isEmpty() {
            return !hasNext();
        }

        public boolean isKeyword() {
            return hasNext() && lexes[index].type()== Type.RESERVED && Character.isAlphabetic(lexes[index].text().charAt(0));
        }

        public boolean isNot(String... textAlternatives) {
            return !is(textAlternatives);
        }

        public boolean isAt(int i, String... textAlternatives) {
            return hasNext(i) && lexes[index + i].is(textAlternatives);
        }

        public boolean is(String... textAlternatives) {
            return hasNext() && lexes[index].is(textAlternatives);
        }

        public boolean isIdentifier() {
            return hasNext() && lexes[index].type()== Type.IDENTIFIER;
        }


    }
}
