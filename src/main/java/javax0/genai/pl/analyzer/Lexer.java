package javax0.genai.pl.analyzer;


import java.util.ArrayList;
import java.util.Set;

public class Lexer {

    final static private Set<String> RESERVED = Set.of(
            //snipline RESERVED
            "class", "final", "fn", "local", "global", "if", "else", "elseif", "then", "endif", "while", "wend", "for", "next", "do", "until", "and", "or", "not", "to", "step", "end"
    );
    final static private String[] OPERANDS = {
            ":=", "==", "!=", "<=", ">=", "<<", "!", "=", "+", "-", "*", "/", "%", "(", ")", "<", ">", ",", ".", "&&", "||",
            "{", "}", "[", "]", ";", ":"
    };

    public Lex.List analyze(javax0.genai.pl.analyzer.Input in) throws BadSyntax {
        final var list = new ArrayList<Lex>();
        while (!in.isEmpty()) {
            if (Character.isWhitespace(in.charAt(0))) {
                in.skip(1);
                continue;
            }
            if (in.charAt(0) == '\'') {
                in.skip(1);
                skipComment(in, list);
                continue;
            }
            if (in.charAt(0) == '`') {
                final var id = StringFetcher.fetchId(in);
                list.add(new Lex(Lex.Type.IDENTIFIER, id));
                continue;
            }
            if (Input.validId1stChar(in.charAt(0))) {
                final var id = in.fetchId();
                if (RESERVED.contains(id)) {
                    list.add(new Lex(Lex.Type.RESERVED, id));
                } else {
                    list.add(new Lex(Lex.Type.IDENTIFIER, id));
                }
                continue;
            }
            if (in.charAt(0) == '"') {
                final var str = javax0.genai.pl.analyzer.StringFetcher.getString(in);
                final var lex = new Lex(Lex.Type.STRING, str);
                list.add(lex);
                continue;
            }
            if (Character.isDigit(in.charAt(0))) {
                final var str = new StringBuilder();
                str.append(in.fetchNumber());
                final Lex.Type type;
                if (!in.isEmpty() && in.charAt(0) == '.') {
                    str.append('.');
                    in.skip(1);
                    str.append(in.fetchNumber());
                    if (!in.isEmpty() && (in.charAt(0) == 'e' || in.charAt(0) == 'E')) {
                        str.append('e');
                        in.skip(1);
                        if (!in.isEmpty() && (in.charAt(0) == '+' || in.charAt(0) == '-')) {
                            str.append(in.charAt(0));
                            in.skip(1);
                        }
                        str.append(in.fetchNumber());
                    }
                    type = Lex.Type.FLOAT;
                } else {
                    type = Lex.Type.INTEGER;
                }
                final var lex = new Lex(type, str.toString());
                list.add(lex);
                continue;
            }
            int operandIndex = in.startsWith(OPERANDS);
            if (operandIndex >= 0) {
                final var lex = new Lex(Lex.Type.RESERVED, OPERANDS[operandIndex]);
                list.add(lex);
                in.skip(OPERANDS[operandIndex].length());
                continue;
            }
            throw new BadSyntax("Unexpected character '" + in.charAt(0) + "' in the input");
        }
        return new Lex.List(list);
    }

    /**
     * Skip the comment until the end of the line. The end of the line is not skipped.
     *
     * @param in   the input
     * @param list the lexical list, where an '\n' is added.
     */
    private static void skipComment(final javax0.genai.pl.analyzer.Input in, final ArrayList<Lex> list) {
        while (!in.isEmpty() && in.charAt(0) != '\n') {
            in.skip(1);
        }
        if (!in.isEmpty()) {
            in.skip(1);
        }
    }
}
