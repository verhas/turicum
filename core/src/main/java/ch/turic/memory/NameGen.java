package ch.turic.memory;

public class NameGen {
    private static final String[] ADJECTIVES = {
            "ficus", "pompus", "suspicius", "novus", "glorificus", "dubius", "ridiculus", "boredus", "confusus", "naughtius",
            "biggus", "brutus", "maximus", "spurius", "incontinentia", "naughtius", "cleavus", "glutus", "leavus", "obvious"
    };

    private static final String[] NOUNS = {
            "clamoria", "deliria", "butucs", "conundrua", "problematica", "bananua", "libertea", "conflicta", "bureaucratia", "hysteria"
    };

    // We do not generate randomly. It is more or less predictable and gives a more coincise naming when
    // debugging the code. In a new run of some simple code, the threads will likely get the same name as in previous runs.
    private static int aIndex = 0;
    private static int nIndex = 0;

    public static String generateName() {
        String adjective = ADJECTIVES[(aIndex++) % ADJECTIVES.length];
        String noun = NOUNS[(nIndex++) % NOUNS.length];
        return adjective + "_" + noun;
    }

    public static void main(String[] args) {
        System.out.println(generateName());
    }
}


