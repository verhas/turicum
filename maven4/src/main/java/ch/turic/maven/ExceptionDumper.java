package ch.turic.maven;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

public class ExceptionDumper {
    /**
     * Processes a throwable and its causes, collecting detailed information about the exception.
     * The method avoids circular references by tracking already-processed throwables using a set.
     * It gathers the throwable's message, stack trace, causes, and suppressed exceptions.
     *
     * @param e the throwable to be processed; can be null. If null, an empty string is returned.
     * @return a string representation of the throwable, including its message, stack trace,
     * causes, and suppressed exceptions.
     */
    public static String dumpException(Throwable e) {
        return dumpException(e, new HashSet<>());
    }

    /**
     * Recursively collects information about a throwable, including its message, stack trace,
     * causes, and suppressed exceptions, while avoiding circular references.
     *
     * @param e         the throwable to be processed; can be null.
     *                  If null or already processed, an empty string is returned.
     * @param processed a set of throwables that have already been processed to prevent
     *                  infinite recursion in case of circular references.
     * @return a string representation of the throwable, including its message and stack trace,
     * as well as information about its causes and suppressed exceptions.
     */
    private static String dumpException(Throwable e, Set<Throwable> processed) {
        if (e == null || processed.contains(e)) {
            return "";
        }
        processed.add(e);
        StringBuilder output = new StringBuilder();
        output.append(e.getMessage()).append("\n");
        try (final var sw = new StringWriter();
             final var pw = new PrintWriter(sw)) {
            e.printStackTrace(pw);
            output.append(sw);
        } catch (IOException ioException) {
            // does not happen, StringWriter does not do anything in close
        }
        output.append(dumpException(e.getCause(), processed));
        for (final Throwable t : e.getSuppressed()) {
            output.append(dumpException(t, processed));
        }
        return output.toString();
    }
}
