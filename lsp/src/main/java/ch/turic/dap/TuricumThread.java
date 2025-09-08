package ch.turic.dap;

// Helper classes
class TuricumThread {
    private final int id;
    private final String name;
    private TuricumStackFrame currentFrame;

    public TuricumThread(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public TuricumStackFrame getCurrentFrame() {
        return currentFrame;
    }

    public void setCurrentFrame(TuricumStackFrame frame) {
        this.currentFrame = frame;
    }
}
