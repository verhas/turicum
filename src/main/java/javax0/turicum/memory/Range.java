package javax0.turicum.memory;

public record Range(Object start, Object end) {

    @Override
    public String toString() {
        return start + ".." + end;
    }

}
