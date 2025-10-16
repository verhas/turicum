package ch.turic.analyzer;

import ch.turic.exceptions.BadSyntax;
import ch.turic.memory.LngList;

import java.util.List;

public class LexList extends LngList {

    private BadSyntax bs = null;

    public BadSyntax getBs() {
        return bs;
    }

    public void setBs(BadSyntax bs) {
        this.bs = bs;
    }

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

    Lex lexAt(int index) {
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
            throw new BadSyntax(lexAt(array.size() - 1).startPosition(), "more elements expected");
        }
        return lexAt(index++);
    }

    public Lex next(Lex.Type expectedType) throws BadSyntax {
        final var lex = next();
        BadSyntax.when(lex.startPosition(), lex.type() != expectedType, "%s was expected and got '%s' which is %s", expectedType.name(), lex.text(), lex.type().name());
        return lex;
    }

    public Lex next(Lex.Type type, String msg) throws BadSyntax {
        if (index >= array.size() || lexAt(index).type() != type) {
            throw new BadSyntax(lexAt(index).startPosition(), msg);
        }
        return next();
    }

    /**
     * Returns the next token if it matches the specified type and text, advancing the index.
     *
     * @param type the expected token type
     * @param text the expected token text
     * @param msg  the error message for the exception if the token does not match
     * @throws BadSyntax if the next token does not match the expected type and text
     */
    public void next(Lex.Type type, String text, String msg) throws BadSyntax {
        if (index >= array.size() || lexAt(index).type() != type || !lexAt(index).text().equals(text)) {
            throw new BadSyntax(lexAt(index).startPosition(), msg);
        }
        next();
    }

    /**
     * Creates a {@code BadSyntax} exception with a formatted message and the current token position,
     * removing the top stack frame from its stack trace.
     *
     * @param msg    the error message format string
     * @param params optional parameters for the message format
     * @return a {@code BadSyntax} exception with a modified stack trace
     */
    public BadSyntax syntaxError(String msg, Object... params) {
        final var e = new BadSyntax(startPosition(), msg, params);
        final var oldSt = e.getStackTrace();
        if (oldSt != null && oldSt.length > 1) {
            final var newSt = new StackTraceElement[oldSt.length - 1];
            System.arraycopy(oldSt, 1, newSt, 0, oldSt.length - 1);
            e.setStackTrace(newSt);
        }
        return e;
    }

    /**
     * Returns the start position of the current token, or the last token if at the end of the list.
     *
     * @return a clone of the current or last token's position
     */
    public Pos startPosition() {
        if (index >= array.size()) {
            if (array.isEmpty()) {
                return null;
            } else {
                return lexAt(array.size() - 1).startPosition().clone();
            }
        }
        return lexAt(index).startPosition().clone();
    }
    /**
     * Returns the start position of the current token, or the last token if at the end of the list.
     *
     * @return a clone of the current or last token's position
     */
    public Pos endPosition() {
        if (index >= array.size()) {
            if (array.isEmpty()) {
                return null;
            } else {
                return lexAt(array.size() - 1).endPosition().clone();
            }
        }
        return lexAt(index).endPosition().clone();
    }

    public void peek(Lex.Type type, String text, String msg) throws BadSyntax {
        if (index >= array.size() || lexAt(index).type() != type || (text != null && !text.equals(lexAt(index).text()))) {
            throw new BadSyntax(lexAt(index).startPosition(), msg);
        }
    }

    public void next(String text, String msg) throws BadSyntax {
        next(Lex.Type.RESERVED, text, msg);
    }

    public Lex peek() throws BadSyntax {
        if (index >= array.size()) {
            throw new BadSyntax(lexAt(array.size() - 1).startPosition(), "more elements expected");
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

    /**
     * Checks if the current lexeme at the specified offset matches any of the provided text alternatives, requiring that
     * these are reserved works or reserved symbols, like {@code if}, {@code ->} and so on.
     *
     * @param i                the offset from the current position to check
     * @param textAlternatives an array of text alternatives to compare the lexeme against
     * @return true if the lexeme at the offset matches any of the text alternatives, false otherwise
     */
    public boolean isAt(int i, String... textAlternatives) {
        return hasNext(i) && lexAt(index + i).is(textAlternatives);
    }

    public boolean isAt(int i, Lex.Type type) {
        return hasNext(i) && lexAt(index + i).type() == type;
    }

    public boolean is(String... s) {
        return hasNext() && lexAt(index).is(s);
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
