package ch.turic.analyzer;

import ch.turic.utils.Unmarshaller;

import java.util.Objects;

public class Pos {

    public final String file;
    public int line;
    public int column;
    public final String[] lines;

    public Pos clone() {
        return new Pos(file, line, column, lines);
    }

    /**
     * Create a new position for the same file and the same lines, but the position
     * is offset with the lines and the columns that are represented by the lexeme text.
     *
     * @param text the textual representation of the lexeme that starts at the current position
     * @return a new position offset by the lexeme
     */
    public Pos offset(final String text) {
        final var nr = (int) text.chars().filter(ch -> ch == '\n').count();
        final var linl = text.lastIndexOf('\n');
        return new Pos(file,
                nr == 0 ? line : line + nr,
                nr == 0 ? column + text.length() : text.length() - linl,
                lines
        );
    }

    public Pos(String file, String[] lines) {
        this.file = file;
        this.lines = lines;
        line = 1;
        column = 1;
    }

    public Pos(String file, int line, int column, String[] lines) {
        this(file, lines);
        this.line = line;
        this.column = column;
    }

    public static Pos factory(final Unmarshaller.Args args) {
        return new Pos(
                args.str("file"),
                Objects.requireNonNullElse(args.get("line", Integer.class), 0),
                Objects.requireNonNullElse(args.get("column", Integer.class), 0),
                args.get("lines", String[].class)
        );
    }
}
