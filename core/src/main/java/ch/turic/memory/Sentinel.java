package ch.turic.memory;

public final class Sentinel {

    private final String name;
    public static final Sentinel FINI = new Sentinel("fini");
    public static final Sentinel NON_MUTAT = new Sentinel("non_mutat");

    private Sentinel(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
