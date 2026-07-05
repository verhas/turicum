package ch.turic.lsp;

import java.io.PrintWriter;

public class ExceptionXmlWriter {

    /**
     * Writes the full throwable structure (stack trace, causes, suppressed exceptions)
     * to the standard error stream. This is a terminal logging call: it must never throw,
     * because the callers use it inside catch blocks as their last-resort error reporting.
     *
     * @param throwable the throwable to log
     */
    public static void writeToXml(Throwable throwable) {
        final var writer = new PrintWriter(System.err);
        writeThrowable(writer, throwable, 1);
        writer.flush();
        System.err.flush();
    }

    private static void writeThrowable(PrintWriter writer, Throwable t, int indentLevel) {
        if (t == null) {
            return;
        }
        String indent = "    ".repeat(indentLevel);

        writer.println(indent + t.getClass().getName());
        if (t.getMessage() != null) {
            writer.println(indent + t.getMessage());
        }

        // Stack trace
        for (StackTraceElement ste : t.getStackTrace()) {
            writer.println(indent + "        " + ste.toString());
        }

        // Suppressed
        Throwable[] suppressed = t.getSuppressed();
        if (suppressed != null && suppressed.length > 0) {
            writer.println(indent + "    suppressed:");
            for (Throwable s : suppressed) {
                writeThrowable(writer, s, indentLevel + 2);
            }
        }

        // Cause
        Throwable cause = t.getCause();
        if (cause != null && cause != t) {
            writer.println(indent + "    cause:");
            writeThrowable(writer, cause, indentLevel + 2);
        }
    }
}
