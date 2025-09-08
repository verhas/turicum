package ch.turic.dap;

class TuricumStackFrame {
    private final int id;
    private final String functionName;
    private String sourcePath;
    private String sourceName;
    private int line;
    private int column;
    private TuricumStackFrame parent;

    public TuricumStackFrame(int id, String functionName, String sourcePath,
                             String sourceName, int line, int column) {
        this.id = id;
        this.functionName = functionName;
        this.sourcePath = sourcePath;
        this.sourceName = sourceName;
        this.line = line;
        this.column = column;
    }

    public int getId() {
        return id;
    }

    public String getFunctionName() {
        return functionName;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public String getSourceName() {
        return sourceName;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public TuricumStackFrame getParent() {
        return parent;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public void setParent(TuricumStackFrame parent) {
        this.parent = parent;
    }
}
