package ch.turic.maven;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.model.io.ModelReader;
import org.codehaus.plexus.util.ReaderFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.Map;

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
    private final ModelReader modelReader;

    public TuriModelProcessor(final ModelReader modelReader) {
        this.modelReader = modelReader;
    }

    public Path locatePom(Path projectDirectory) {
        PomXmlCreator.turi2Xml(projectDirectory.toFile());
        return projectDirectory.resolve("pom.xml").toAbsolutePath();
    }

    public Path locateExistingPom(Path project) {
        return locatePom(project);
    }

    @Override
    public File locatePom(File projectDirectory) {

        PomXmlCreator.turi2Xml(projectDirectory);
        return new File(projectDirectory, "pom.xml");
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