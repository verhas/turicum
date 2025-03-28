package javax0.turicum.memory;

public record InfinitValue(boolean positive) {
    public static final InfinitValue INF_POSITIVE = new InfinitValue(true);
    public static final InfinitValue INF_NEGATIVE = new InfinitValue(false);

    public static boolean is(Object value) {
        return value instanceof InfinitValue;
    }

    public InfinitValue negate(){
        if( positive ){
            return INF_NEGATIVE;
        }else {
            return INF_POSITIVE;
        }
    }

    @Override
    public String toString() {
        return (positive ? "" : "-") + "inf";
    }

}
