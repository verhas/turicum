package ch.turic.analyzer;

public class Pos {

    public final String file;
    public int line;
    public int column;
    public String[] lines;

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
}
