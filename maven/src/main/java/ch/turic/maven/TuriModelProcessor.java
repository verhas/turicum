package ch.turic.maven;

import ch.turic.Interpreter;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.model.io.ModelParseException;
import org.apache.maven.model.io.ModelReader;
import org.codehaus.plexus.util.ReaderFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * Custom implementation of the {@code ModelProcessor} interface that provides
 * methods for locating POM files, reading model data, and converting custom configuration
 * files into POM XML files. This class uses a {@code ModelReader} component for reading
 * models and implements additional processing for custom formats.
 * <p>
 * This class is invoked by Maven 3. x.x only and not by Maven 4.0.0
 */
@Named
@Singleton
public class TuriModelProcessor implements ModelProcessor {
    @Inject
    private ModelReader modelReader;

    public Path locatePom(Path projectDirectory) {
        turi2Xml(projectDirectory.toFile());
        return projectDirectory.resolve("pom.xml").toAbsolutePath();
    }

    public Path locateExistingPom(Path project) {
        return locatePom(project);
    }

    @Override
    public File locatePom(File projectDirectory) {

        turi2Xml(projectDirectory);
        return new File(projectDirectory, "pom.xml");
    }

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
        File turiFile = new File(directory, "pom.turi");
        if (!turiFile.exists()) {
            throw new RuntimeException("There is no 'pom.turi' file.");
        }

        final String fileName = turiFile.getAbsolutePath();
        final String xml;
        try(final var interpreter = new Interpreter(Path.of(fileName))){
            xml = interpreter.compileAndExecute().toString();
        } catch (Exception e) {
            throw new RuntimeException("Jamal error processing the file " + fileName + "\n" + dumpException(e), e);
        }
        String formattedXml;
        try {
            formattedXml = formatOutput(xml);
        } catch (Exception e) {
            throw new RuntimeException("Cannot format the file " + fileName + "\n" + dumpException(e), e);
        }

        final File output = new File(directory, "pom.xml");
        // noinspection ResultOfMethodCallIgnored
        output.setWritable(true);
        try (final OutputStream os = new FileOutputStream(output)) {
            os.write(formattedXml.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Cannot write the 'pom.xml' file.", e);
        }
    }

    /**
     * Processes a throwable and its causes, collecting detailed information about the exception.
     * The method avoids circular references by tracking already-processed throwables using a set.
     * It gathers the throwable's message, stack trace, causes, and suppressed exceptions.
     *
     * @param e the throwable to be processed; can be null. If null, an empty string is returned.
     * @return a string representation of the throwable, including its message, stack trace,
     * causes, and suppressed exceptions.
     */
    private static String dumpException(Throwable e) {
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

    /**
     * Reads a model from the provided input stream and processes it using the specified options.
     * This method utilizes a platform-specific reader to handle the input stream.
     *
     * @param input   the InputStream from which the model data is read; must not be null
     * @param options a map of options that influence the model reading process; can be null
     * @return the constructed {@code Model} object based on the data read from the input stream
     * @throws IOException if an I/O error occurs while reading the input stream
     */
    @Override
    public Model read(InputStream input, Map<String, ?> options) throws IOException {
        try (final Reader in = ReaderFactory.newPlatformReader(input)) {
            return read(in, options);
        }
    }

    /**
     * Reads a model from the given path input and processes it using the specified options.
     * This method converts the {@code Path} input to a {@code File} before delegating
     * the processing to another method.
     *
     * @param input   the path from which the model data is read; must not be null
     * @param options a map of options that influence the model reading process; can be null
     * @return the constructed {@code Model} object based on the data read from the input
     * @throws IOException         if an I/O error occurs while reading the input
     * @throws ModelParseException if the model cannot be parsed
     */
    public Model read(Path input, Map<String, ?> options) throws IOException, ModelParseException {
        return read(input.toFile(), options);
    }

    /**
     * Reads a model from the provided reader and processes it using the specified options.
     * This method delegates the reading process to an underlying model reader.
     *
     * @param reader  the {@code Reader} from which the model data is read; must not be null
     * @param options a map of options that influence the model reading process; can be null
     * @return the constructed {@code Model} object based on the data read from the reader
     * @throws IOException if an I/O error occurs while reading from the reader
     */
    @Override
    public Model read(Reader reader, Map<String, ?> options) throws IOException {
        return modelReader.read(reader, options);
    }

    /**
     * Reads a model from the given file input and processes it with the specified options.
     *
     * @param input   the file from which the model data is read
     * @param options a map of options that influence the model reading process
     * @return the constructed {@code Model} object from the input file
     * @throws IOException if an I/O error occurs during reading
     */
    @Override
    public Model read(File input, Map<String, ?> options) throws IOException {
        return read(new FileInputStream(input), options);
    }
}