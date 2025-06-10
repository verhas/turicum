package ch.turic.utils;

import ch.turic.ExecutionException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Glob {

    public static List<String> glob(String pattern, final boolean recurse) throws IOException {
        DirNPath dirNPath = splitToDirAndPattern(pattern);
        Path root = Path.of(dirNPath.baseDir());
        if (recurse) {
            return globRecursive(dirNPath.matcher(), root, Integer.MAX_VALUE);
        } else {
            return globRecursive(dirNPath.matcher(), root, 1);
        }
    }

    /**
     * Splits a given file pattern into its base directory and a pattern matcher.
     * If the pattern includes a slash ('/'), the path up to the last slash
     * before any wildcard characters is considered the base directory, and the remainder
     * is treated as the pattern. If no slash is present, the current directory is
     * used as the base directory.
     *
     * @param pattern the file pattern, potentially including wildcards such as '*' or '?'
     * @return a DirNPath object that contains the base directory and the corresponding pattern matcher
     */
    private static DirNPath splitToDirAndPattern(String pattern) {
        final String baseDir;
        final Predicate<String> matcher;

        if (pattern.contains("/")) {
            int lastSlash = getLastSlashBeforeWildCard(pattern);
            if (lastSlash == -1) {
                baseDir = Path.of(".").normalize().toAbsolutePath().toString();
                matcher = new Matcher(pattern, baseDir);
            } else {
                baseDir = Path.of(pattern.substring(0, lastSlash)).normalize().toAbsolutePath().toString();
                pattern = pattern.substring(lastSlash + 1);
                matcher = new Matcher(pattern, baseDir);
            }
        } else {
            baseDir = Path.of(".").normalize().toAbsolutePath().toString();
            matcher = new Matcher(pattern, baseDir);
        }
        return new DirNPath(baseDir, matcher);
    }

    private record DirNPath(String baseDir, Predicate<String> matcher) {
    }

    public static class Matcher implements Predicate<String> {
        final String pattern;
        final String baseDir;

        public Matcher(String pattern, String baseDir) {
            this.pattern = pattern;
            this.baseDir = baseDir;
        }

        @Override
        public boolean test(String s) {
            final String relative;
            if (s.startsWith(baseDir + "/")) {
                relative = s.substring(baseDir.length() + 1);
            } else if (s.startsWith(baseDir)) {
                relative = "";
            } else {
                throw new ExecutionException("Invalid test path : '" + s + "' for base dir '" + baseDir + "'");
            }

            return StringUtils.matches(pattern, relative);
        }
    }

    /**
     * Determines the position of the last slash ('/') in the given pattern before the first wildcard character.
     * Wildcard characters are defined as '*' or '?'.
     * If there are no wildcard characters in the input, the position of the last slash in the pattern is returned.
     *
     * @param pattern the input string pattern to search for slashes and wildcard characters
     * @return the index of the last slash ('/') before the first wildcard character,
     * or the index of the last slash in the entire pattern if no wildcard exists
     */
    private static int getLastSlashBeforeWildCard(String pattern) {
        final int star = pattern.indexOf("*");
        final int qm = pattern.indexOf("?");
        final int firstWildcard;
        if (qm == -1) {
            firstWildcard = star;
        } else if (star == -1) {
            firstWildcard = qm;
        } else {
            firstWildcard = Math.min(star, qm);
        }
        if (firstWildcard == -1) {
            return pattern.lastIndexOf('/');
        }
        return pattern.substring(0, firstWildcard).lastIndexOf('/');
    }


    /**
     * Recursively searches for files within a directory that match the given {@link PathMatcher}.
     * This method traverses all files under the specified root directory and filters them
     * using the provided path matcher.
     *
     * @param matcher the {@link PathMatcher} used to determine if a file matches the desired pattern
     * @param root    the root directory from which to start the recursive search
     * @return a list of {@link Path} objects representing files that match the specified pattern
     * @throws IOException if an I/O error is encountered while accessing the file system
     */
    public static List<String> globRecursive(final Predicate<String> matcher, final Path root, final int depth) throws IOException {
        final var baseDir = root.normalize().toAbsolutePath().toString();
        try (Stream<Path> stream = Files.walk(root, depth)) {
            return stream
                    .map(path -> path.normalize().toAbsolutePath().toString())
                    .filter(matcher)
                    .map(s -> Files.isRegularFile(Path.of(s)) ? s : s + "/")
                    .filter(s -> !s.equals(baseDir))
                    .map(s -> s.substring(baseDir.length() + 1))
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
    }

}
