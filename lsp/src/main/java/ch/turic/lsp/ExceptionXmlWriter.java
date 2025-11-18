package ch.turic.lsp;

import java.io.PrintWriter;

public class ExceptionXmlWriter {

    /**
     * Writes the full throwable structure (stack trace, causes, suppressed exceptions)
     * into an XML file.
     *
     * @param throwable the throwable to serialize
     */
    public static void writeToXml(Throwable throwable) {
        try (PrintWriter writer = new PrintWriter(System.err)) {
            writeThrowable(writer, throwable, 1);
        }
        System.err.flush();
        throw new RuntimeException(throwable);
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
