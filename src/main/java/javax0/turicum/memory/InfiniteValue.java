package javax0.turicum.memory;

public record InfiniteValue(boolean positive) {

    public static final InfiniteValue INF_POSITIVE = new InfiniteValue(true);
    public static final InfiniteValue INF_NEGATIVE = new InfiniteValue(false);

    public static boolean is(Object value) {
        return value instanceof InfiniteValue;
    }

    public InfiniteValue negate() {
        if (positive) {
            return INF_NEGATIVE;
        } else {
            return INF_POSITIVE;
        }
    }

    @Override
    public String toString() {
        return (positive ? "" : "-") + "inf";
    }

}
