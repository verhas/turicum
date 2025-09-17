package ch.turic.analyzer;


import ch.turic.ExecutionException;
import ch.turic.memory.HasFields;

import java.util.Set;

public class Lex implements HasFields {
    Type type;
    String text;
    String lexeme; // the original a of the token including ", spaces, escapes etc.
    final Pos position;
    boolean atLineStart;
    final public boolean interpolated;

    public Pos position() {
        return position;
    }

    public boolean atLineStart() {
        return atLineStart;
    }

    public String text() {
        return text;
    }

    public String lexeme() {
        return lexeme;
    }

    public Type type() {
        return type;
    }

    public static Lex string(String text, String lexeme, boolean atLineStart, Pos position) {
        return new Lex(Type.STRING, text, lexeme, atLineStart, position, lexeme.startsWith("$"));
    }


    public static Lex character(Input in, boolean atLineStart, Pos position) {
        return new Lex(Lex.Type.CHARACTER, in.substring(0, 1), atLineStart, position);
    }

    public static Lex reserved(String text, boolean atLineStart, Pos position) {
        return new Lex(Type.RESERVED, text, atLineStart, position);
    }

    public static Lex identifier(String text, boolean atLineStart, Pos position) {
        return identifier(text, text, atLineStart, position);
    }

    public static Lex identifier(String text, String lexeme, boolean atLineStart, Pos position) {
        return new Lex(Type.IDENTIFIER, text, lexeme, atLineStart, position);
    }

    public Lex(Type type, String text, String lexeme, boolean atLineStart, Pos position, boolean interpolated) {
        this.type = type;
        this.text = text;
        this.lexeme = lexeme;
        this.atLineStart = atLineStart;
        this.position = position.clone();
        this.interpolated = interpolated;
    }

    public Lex(Type type, String text, String lexeme, boolean atLineStart, Pos position) {
        this(type, text, lexeme, atLineStart, position, false);
    }

    public Lex(Type type, String text, boolean atLineStart, Pos position) {
        this(type, text, text, atLineStart, position, false);
    }

    @Override
    public String toString() {
        return String.format("Lex{type=%s, text='%s', atLineStart=%s}", type, text, "" + atLineStart);
    }

    @Override
    public void setField(String name, Object value) throws ExecutionException {
        switch (name) {
            case "type":
                type = (Type) value;
                break;
            case "text":
                text = (String) value;
                break;
            case "atLineStart":
                atLineStart = (Boolean) value;
                break;
            default:
                throw new ExecutionException("Unknown field: " + name);
        }
    }

    @Override
    public Object getField(String name) throws ExecutionException {
        return switch (name) {
            case "type" -> type;
            case "text" -> text;
            case "atLineStart" -> atLineStart;
            default -> throw new ExecutionException("Unknown field: " + name);
        };
    }

    @Override
    public Set<String> fields() {
        return Set.of("type", "text", "atLineStart");
    }

    public enum Type {
        IDENTIFIER, RESERVED, STRING, INTEGER, FLOAT,
        // these are only collected when parsing for all
        COMMENT, // the comment
        SPACES, // white spaces between tokens, if any
        CHARACTER // general character that is not defined in any way, an error when it happens
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

}
