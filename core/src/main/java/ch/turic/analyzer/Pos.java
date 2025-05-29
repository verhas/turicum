package ch.turic.analyzer;

import ch.turic.utils.Unmarshaller;

public class Pos {

    public final String file;
    public int line;
    public int column;
    public final String[] lines;

    public Pos clone(){
        return new Pos(file, line, column, lines);
    }

    public Pos(String file, String[] lines) {
        this.file = file;
        this.lines = lines;
        line = 1;
        column = 1;
    }
    public Pos(String file, int line, int column,String[] lines) {
        this(file,lines);
        this.line = line;
        this.column = column;
    }

    public static Pos factory(final Unmarshaller.Args args) {
        return new Pos(
                args.str("file"),
                args.get("line",Integer.class),
                args.get("column",Integer.class),
                args.get("lines", String[].class)
        );
    }
}
