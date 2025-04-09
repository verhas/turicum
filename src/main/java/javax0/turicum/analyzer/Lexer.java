package javax0.turicum.analyzer;


import javax0.turicum.BadSyntax;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Lexer {

    final static private Set<String> RESERVED = new HashSet<>(Set.of(
            Keywords.CLASS, Keywords.PIN, Keywords.FN, Keywords.LET, Keywords.GLOBAL, Keywords.IF, Keywords.ELSE,
            Keywords.ELSEIF, Keywords.BREAK, Keywords.WHILE, Keywords.UNTIL, Keywords.FOR, Keywords.EACH, Keywords.IN,
            Keywords.RETURN, Keywords.YIELD, Keywords.WHEN, Keywords.TRY, Keywords.CATCH, Keywords.FINALLY
    ));
    final static private ArrayList<String> _OPERANDS = new ArrayList<>(Arrays.asList(
            "->", "=", "(", ")", ",", ".",
            "{", "}", "[", "]", ";", ":", "|", "?", "@", "^", "#"
    ));

    static {
        Arrays.stream(BinaryExpressionAnalyzer.binaryOperators).flatMap(Arrays::stream).forEach(
                s -> {
                    if (Character.isAlphabetic(s.charAt(0))) {
                        RESERVED.add(s);
                    } else {
                        _OPERANDS.add(s);
                    }
                }
        );
        Arrays.stream(UnaryExpressionAnalyzer.unaryOperators).forEach(
                s -> {
                    if (Character.isAlphabetic(s.charAt(0))) {
                        RESERVED.add(s);
                    } else {
                        _OPERANDS.add(s);
                    }
                }
        );
        _OPERANDS.sort((a, b) -> Integer.compare(b.length(), a.length()));
    }

    private static final String[] OPERANDS = _OPERANDS.toArray(String[]::new);

    private static final String[] uniKeys = {
            "\u221E", "inf",
            "\u2205", "none"
    };

    private static String getUnicodeKeyword(String ch) {
        for (int i = 0; i < uniKeys.length; i += 2) {
            if (ch.equals(uniKeys[i])) {
                return uniKeys[i + 1];
            }
        }
        return null;
    }

    private static final String[] uniSymbols = {
            "\u2026", "..",
            "\u2192", "->",
            "\u2260", "!="
    };

    private static String getUnicodeSymbol(String ch) {
        for (int i = 0; i < uniSymbols.length; i += 2) {
            if (ch.equals(uniSymbols[i])) {
                return uniSymbols[i + 1];
            }
        }
        return null;
    }

    public static LexList analyze(Input in) throws BadSyntax {
        final var list = new ArrayList<Lex>();
        while (!in.isEmpty()) {
            boolean atLineStart = false;// the first line start does not matter
            while (!in.isEmpty() && in.charAt(0) == '\n') {
                atLineStart = true;
                in.skip(1);
            }
            while (!in.isEmpty() && Character.isWhitespace(in.charAt(0))) {
                in.skip(1);
            }
            if (in.isEmpty()) {
                break;
            }
            if (in.length() >= 2 && in.charAt(0) == '/' && in.charAt(1) == '*') {
                in.skip(2);
                skipMLComment(in);
                continue;
            }
            if (in.length() >= 2 && in.charAt(0) == '/' && in.charAt(1) == '/') {
                skipComment(in);
                continue;
            }
            if (in.charAt(0) == '`') {
                final var id = StringFetcher.fetchId(in);
                list.add(new Lex(Lex.Type.IDENTIFIER, id, atLineStart));
                continue;
            }
            final var uniKeyword = getUnicodeKeyword("" + in.charAt(0));
            if (uniKeyword != null) {
                if (RESERVED.contains(uniKeyword)) {
                    list.add(new Lex(Lex.Type.RESERVED, uniKeyword, atLineStart));
                } else {
                    list.add(new Lex(Lex.Type.IDENTIFIER, uniKeyword, atLineStart));
                }
                in.skip(1);
                continue;
            }
            if (Input.validId1stChar(in.charAt(0))) {
                final var id = in.fetchId();
                if (RESERVED.contains(id)) {
                    list.add(new Lex(Lex.Type.RESERVED, id, atLineStart));
                } else {
                    list.add(new Lex(Lex.Type.IDENTIFIER, id, atLineStart));
                }
                continue;
            }
            if (in.charAt(0) == '"') {
                final var str = javax0.turicum.analyzer.StringFetcher.getString(in);
                final var lex = new Lex(Lex.Type.STRING, str, atLineStart);
                list.add(lex);
                continue;
            }
            if (Character.isDigit(in.charAt(0))) {
                final var str = new StringBuilder();
                str.append(in.fetchNumber());
                final Lex.Type type;
                if (in.length() >= 2 && in.charAt(0) == '.' && Character.isDigit(in.charAt(1))) {
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
                final var lex = new Lex(type, str.toString(), atLineStart);
                list.add(lex);
                continue;
            }
            final var uniSym = getUnicodeSymbol("" + in.charAt(0));
            if (uniSym != null) {
                final var lex = new Lex(Lex.Type.RESERVED, uniSym, atLineStart);
                list.add(lex);
                in.skip(1);
                continue;
            }
            int operandIndex = in.startsWith(OPERANDS);
            if (operandIndex >= 0) {
                final var lex = new Lex(Lex.Type.RESERVED, OPERANDS[operandIndex], atLineStart);
                list.add(lex);
                in.skip(OPERANDS[operandIndex].length());
                continue;
            }
            throw new BadSyntax("Unexpected character '" + in.charAt(0) + "' in the input");
        }
        return new LexList(list);
    }

    /**
     * Skip the multi-line comment, can be nested
     *
     * @param in the input
     */
    private static void skipMLComment(final Input in) {
        while (in.length() >= 2 && (in.charAt(0) != '*' || in.charAt(1) != '/')) {
            if (in.charAt(0) == '/' && in.charAt(1) == '*') {
                in.skip(2);
                skipMLComment(in);
            } else {
                in.skip(1);
            }
        }
        if (!in.isEmpty()) {
            in.skip(1);
        }
        if (!in.isEmpty()) {
            in.skip(1);
        }
    }

    /**
     * Skip the comment until the end of the line. The end of the line is not skipped.
     *
     * @param in the input
     */
    private static void skipComment(final Input in) {
        while (!in.isEmpty() && in.charAt(0) != '\n') {
            in.skip(1);
        }
    }
}
