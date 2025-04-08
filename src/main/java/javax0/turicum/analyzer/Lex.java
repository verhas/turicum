package javax0.turicum.analyzer;


import javax0.turicum.BadSyntax;

public record Lex(Type type, String text, boolean atLineStart) {

    @Override
    public String toString() {
        return String.format("Lex{types=%s, text='%s'}", type, text);
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
