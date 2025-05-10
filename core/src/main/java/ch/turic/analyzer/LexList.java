package ch.turic.analyzer;

import ch.turic.BadSyntax;
import ch.turic.memory.LngList;

import java.util.List;

public class LexList extends LngList {

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
     * Purge the already used up elements of the lexical analyzer.
     * This is used by the preprocessing before passing the LexList to the preprocessor closure.
     */
    public void purge() {
        array.subList(0, index).clear();
        index = 0;
    }

    private Lex lexAt(int index) {
        return (Lex) array.get(index);
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

    public LexList(final List<Lex> lexes) {
        array.addAll(lexes);
    }

    public Lex next() throws BadSyntax {
        if (index >= array.size()) {
            throw new BadSyntax(lexAt(array.size() - 1).position(), "more elements expected");
        }
        return lexAt(index++);
    }

    public Lex next(Lex.Type expectedType) throws BadSyntax {
        final var lex = next();
        BadSyntax.when(lex.position(), lex.type() != expectedType, "%s was expected and got '%s' which is %s", expectedType.name(), lex.text(), lex.type().name());
        return lex;
    }

    public Lex next(Lex.Type type, String msg) throws BadSyntax {
        if (index >= array.size() || lexAt(index).type() != type) {
            throw new BadSyntax(lexAt(index).position(), msg);
        }
        return next();
    }

    public Lex next(Lex.Type type, String text, String msg) throws BadSyntax {
        if (index >= array.size() || lexAt(index).type() != type || !lexAt(index).text().equals(text)) {
            throw new BadSyntax(lexAt(index).position(), msg);
        }
        return next();
    }

    public BadSyntax syntaxError(String msg, Object... params) {
        return new BadSyntax(position(), msg, params);
    }

    public Pos position() {
        if (index >= array.size()) {
            return lexAt(array.size() - 1).position();
        }
        return lexAt(index).position().clone();
    }

    public void peek(Lex.Type type, String text, String msg) throws BadSyntax {
        if (index >= array.size() || lexAt(index).type() != type || (text != null && !text.equals(lexAt(index).text()))) {
            throw new BadSyntax(lexAt(index).position(), msg);
        }
    }

    public void next(String text, String msg) throws BadSyntax {
        next(Lex.Type.RESERVED, text, msg);
    }

    public Lex peek() throws BadSyntax {
        if (index >= array.size()) {
            throw new BadSyntax(lexAt(array.size() - 1).position(), "more elements expected");
        }
        return lexAt(index);
    }

    public boolean hasNext() {
        return index < array.size();
    }

    public boolean hasNext(int i) {
        return index + i < array.size();
    }

    public boolean isEmpty() {
        return !hasNext();
    }

    public boolean isKeyword() {
        return hasNext() && lexAt(index).type() == Lex.Type.RESERVED && Character.isAlphabetic(lexAt(index).text().charAt(0));
    }

    public boolean isNot(String... textAlternatives) {
        return !is(textAlternatives);
    }

    public boolean isAt(int i, String... textAlternatives) {
        return hasNext(i) && lexAt(index + i).is(textAlternatives);
    }

    public boolean isAt(int i, Lex.Type type) {
        return hasNext(i) && lexAt(index + i).type() == type;
    }

    public boolean is(String... textAlternatives) {
        return hasNext() && lexAt(index).is(textAlternatives);
    }

    public boolean isIdentifier() {
        return hasNext() && lexAt(index).type() == Lex.Type.IDENTIFIER;
    }

    public boolean isIdentifier(String... textAlternatives) {
        if (!isIdentifier()) return false;
        final var text = lexAt(index).text();
        for (final var s : textAlternatives)
            if (text.equals(s)) {
                return true;
            }
        return false;
    }

    public boolean isConstant() {
        return hasNext() && (lexAt(index).type() == Lex.Type.STRING ||
                lexAt(index).type() == Lex.Type.FLOAT ||
                lexAt(index).type() == Lex.Type.INTEGER);
    }


}
