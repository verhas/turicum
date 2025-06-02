package ch.turic.maven;

import ch.turic.analyzer.Input;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.model.io.ModelReader;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.ReaderFactory;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This is the extension class that implements the maven macro extension
 */
@Component(role = ModelProcessor.class)
public class CustomModelProcessor implements ModelProcessor {
    @Requirement
    private ModelReader modelReader;

    @Override
    public File locatePom(File projectDirectory) {

        turi2Xml(projectDirectory);
        return new File(projectDirectory, "pom.xml");
    }

    private void turi2Xml(final File directory) {
        File turiFile = new File(directory, "pom.turi");
        if (!turiFile.exists()) {
            throw new RuntimeException("There is no 'pom.turi' file.");
        }

        final String fileName = turiFile.getAbsolutePath();
        final String xml;
        try {
            final var interpreter = new ch.turic.Interpreter((Input)ch.turic.Input.fromFile(Path.of(fileName)));
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

    private String dumpException(Throwable e) {
        return dumpException(e, new HashSet<>());
    }

    private String dumpException(Throwable e, Set<Throwable> processed) {
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

    private String formatOutput(String result) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new InputSource(new StringReader(result)));
        Transformer tf = TransformerFactory.newInstance().newTransformer();
        tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        tf.setOutputProperty(OutputKeys.INDENT, "yes");
        Writer out = new StringWriter();
        tf.transform(new DOMSource(doc), new StreamResult(out));
        return Arrays.stream(out.toString().split(System.lineSeparator())).filter(s -> s.trim().length() > 0).collect(Collectors.joining(System.lineSeparator()));
    }

    @Override
    public Model read(InputStream input, Map<String, ?> options) throws IOException {
        try (final Reader in = ReaderFactory.newPlatformReader(input)) {
            return read(in, options);
        }
    }

    @Override
    public Model read(Reader reader, Map<String, ?> options) throws IOException {
        return modelReader.read(reader, options);
    }

    @Override
    public Model read(File input, Map<String, ?> options) throws IOException {
        return read(new FileInputStream(input), options);
    }
}