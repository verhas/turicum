package ch.turic.memory;

import java.util.Random;

public class NameGen {
    private static final String[] ADJECTIVES = {
            "ficus", "pompus", "suspicius", "novus", "glorificus", "dubius", "ridiculus", "boredus", "confusus", "naughtius",
            "biggus", "brutus", "maximus", "spurius", "incontinentia", "naughtius", "cleavus", "glutus", "leavus", "obvious"
    };

    private static final String[] NOUNS = {
            "clamoria", "deliria", "butucs", "conundrua", "problematica", "bananua", "libertea", "conflicta", "bureaucratia", "hysteria"
    };

    private static final Random RANDOM = new Random();

    public static String generateName() {
        String adjective = ADJECTIVES[RANDOM.nextInt(ADJECTIVES.length)];
        String noun = NOUNS[RANDOM.nextInt(NOUNS.length)];
        return adjective + "_" + noun;
    }

    public static void main(String[] args) {
        System.out.println(generateName());
    }
}


