package javax0.turicum.analyzer;


import javax0.turicum.ExecutionException;
import javax0.turicum.memory.HasFields;

public class Lex implements HasFields {
    Type type;
    String text;

    public boolean atLineStart() {
        return atLineStart;
    }

    public String text() {
        return text;
    }

    public Type type() {
        return type;
    }

    boolean atLineStart;

    public Lex(Type type, String text, boolean atLineStart) {
        this.type = type;
        this.text = text;
        this.atLineStart = atLineStart;
    }

    @Override
    public String toString() {
        return String.format("Lex{types=%s, text='%s'}", type, text);
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
        return switch (name){
            case "type" -> type;
            case "text" -> text;
            case "atLineStart" -> atLineStart;
            default -> throw new ExecutionException("Unknown field: " + name);
        };
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

}
