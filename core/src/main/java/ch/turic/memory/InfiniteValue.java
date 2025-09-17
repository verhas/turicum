package ch.turic.memory;

/**
 * Represents a special value for positive or negative infinity.
 * This class provides a mechanism to denote and operate on infinite values
 * in a context where such values are meaningful, such as ranges or bounds.
 * <p>
 * Instances of InfiniteValue may be used to represent either positive infinity
 * ({@code InfiniteValue.INF_POSITIVE}) or negative infinity
 * ({@code InfiniteValue.INF_NEGATIVE}).
 */
public record InfiniteValue(boolean positive) {
    public static final InfiniteValue INF_POSITIVE = new InfiniteValue(true);
    public static final InfiniteValue INF_NEGATIVE = new InfiniteValue(false);

    /**
     * Checks whether the provided value is an instance of {@link InfiniteValue}.
     *
     * @param value the object to check, which may or may not be an instance of {@link InfiniteValue}
     * @return {@code true} if the provided value is an instance of {@link InfiniteValue}, otherwise {@code false}
     */
    public static boolean is(Object value) {
        return value instanceof InfiniteValue;
    }

    /**
     * Reverses the sign of the current infinite value instance.
     * If the instance represents positive infinity, the result will be negative infinity.
     * Conversely, if the instance represents negative infinity, the result will be positive infinity.
     *
     * @return the negated infinite value instance (positive infinity becomes negative infinity, and vice versa)
     */
    public InfiniteValue negate() {
        if (positive) {
            return INF_NEGATIVE;
        } else {
            return INF_POSITIVE;
        }
    }

    /**
     * Returns the string representation of the infinite value instance.
     * The output is "inf" for positive infinity and "-inf" for negative infinity.
     *
     * @return a string representation of the infinite value, either "inf" or "-inf"
     */
    @Override
    public String toString() {
        return (positive ? "" : "-") + "inf";
    }

}
