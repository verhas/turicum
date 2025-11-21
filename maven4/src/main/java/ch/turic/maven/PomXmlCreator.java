package ch.turic.maven;

import ch.turic.Interpreter;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Collectors;

import static ch.turic.maven.FileDebugLogger.debug;

public class PomXmlCreator {
    /**
     * Converts the 'pom.turi' file located in the specified directory into a formatted
     * 'pom.xml' file. The method reads, compiles, formats, and writes the XML output.
     * If the 'pom.turi' file is not found, an exception is thrown. Exceptions during
     * processing, formatting, or writing are also captured and re-thrown.
     *
     * @param directory the directory containing the 'pom.turi' file; must not be null
     *                  and should have read-write access, because the `pom.xml` will also be created there
     * @throws RuntimeException if the 'pom.turi' file is not found, cannot be processed,
     *                          cannot be formatted, or the output file cannot be written
     */
    public static void turi2Xml(final File directory) {
        debug("Processing directory:" + directory);
        File turiFile = new File(directory, "pom.turi");
        if (!turiFile.exists()) {
            throw new RuntimeException("There is no 'pom.turi' file.");
        }

        final String fileName = turiFile.getAbsolutePath();
        final String xml;
        try (final var interpreter = new Interpreter(Path.of(fileName))) {
            xml = interpreter.compileAndExecute().toString();
        } catch (Exception e) {
            throw new RuntimeException("Turicum error processing the file " + fileName + "\n" + ExceptionDumper.dumpException(e), e);
        }
        final String formattedXml;
        try {
            formattedXml = formatOutput(xml) +
                    "\n<!-- Generated on: " + ZonedDateTime.now().format(DateTimeFormatter.ISO_ZONED_DATE_TIME) + " -->\n";
        } catch (Exception e) {
            throw new RuntimeException("Cannot format the file " + fileName + "\n" + ExceptionDumper.dumpException(e), e);
        }

        final File output = new File(directory, "pom.xml");
        if (!output.setWritable(true)) {
            throw new RuntimeException("Cannot make pom.xml writable");

        }
        try (final OutputStream os = new FileOutputStream(output)) {
            os.write(formattedXml.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Cannot write the 'pom.xml' file.", e);
        }
    }

    /**
     * Formats a given XML string by ensuring proper indentation and encoding.
     * This method parses the input XML string, transforms it into a structured
     * XML format with indentation, and removes any empty lines.
     *
     * @param result the input XML string to be formatted
     * @return the formatted XML string with proper indentation and encoding
     * @throws Exception if an error occurs during XML parsing or transformation
     */
    private static String formatOutput(String result) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new InputSource(new StringReader(result)));
        Transformer tf = TransformerFactory.newInstance().newTransformer();
        tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        tf.setOutputProperty(OutputKeys.INDENT, "yes");
        Writer out = new StringWriter();
        tf.transform(new DOMSource(doc), new StreamResult(out));
        return Arrays.stream(out.toString().split(System.lineSeparator())).filter(s -> !s.trim().isEmpty()).collect(Collectors.joining(System.lineSeparator()));
    }
}
