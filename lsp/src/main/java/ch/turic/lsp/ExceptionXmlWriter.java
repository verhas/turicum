package ch.turic.lsp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class ExceptionXmlWriter {

    /**
     * Writes the full throwable structure (stack trace, causes, suppressed exceptions)
     * into an XML file.
     *
     * @param throwable the throwable to serialize
     */
    public static void writeToXml(Throwable throwable){
        try (PrintWriter writer = new PrintWriter(System.err)) {
            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.println("<exception>");
            writeThrowable(writer, throwable, 1);
            writer.println("</exception>");
        }
        throw new RuntimeException(throwable);
    }

    private static void writeThrowable(PrintWriter writer, Throwable t, int indentLevel) {
        if (t == null) {
            return;
        }
        String indent = "    ".repeat(indentLevel);

        writer.println(indent + "<throwable>");
        writer.println(indent + "    <type>" + xmlEscape(t.getClass().getName()) + "</type>");
        if (t.getMessage() != null) {
            writer.println(indent + "    <message>" + xmlEscape(t.getMessage()) + "</message>");
        }

        // Stack trace
        writer.println(indent + "    <stack-trace>");
        for (StackTraceElement ste : t.getStackTrace()) {
            writer.println(indent + "        <frame>" + xmlEscape(ste.toString()) + "</frame>");
        }
        writer.println(indent + "    </stack-trace>");

        // Suppressed
        Throwable[] suppressed = t.getSuppressed();
        if (suppressed != null && suppressed.length > 0) {
            writer.println(indent + "    <suppressed>");
            for (Throwable s : suppressed) {
                writeThrowable(writer, s, indentLevel + 2);
            }
            writer.println(indent + "    </suppressed>");
        }

        // Cause
        Throwable cause = t.getCause();
        if (cause != null && cause != t) {
            writer.println(indent + "    <cause>");
            writeThrowable(writer, cause, indentLevel + 2);
            writer.println(indent + "    </cause>");
        }

        writer.println(indent + "</throwable>");
    }

    private static String xmlEscape(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
