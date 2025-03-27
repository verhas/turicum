package javax0.turicum.memory;

public record Range(long start, long end) {

    @Override
    public String toString() {
        return start + ".." + end;
    }
}
