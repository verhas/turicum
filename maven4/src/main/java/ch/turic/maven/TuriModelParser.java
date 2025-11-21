package ch.turic.maven;

import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.Source;
import org.apache.maven.api.services.Sources;
import org.apache.maven.api.services.xml.ModelXmlFactory;
import org.apache.maven.api.spi.ModelParser;
import org.apache.maven.api.spi.ModelParserException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static ch.turic.maven.PomXmlCreator.turi2Xml;

@Singleton
@Named("turicum")
public class TuriModelParser implements ModelParser {

    @Inject
    private final ModelXmlFactory xmlFactory;

    public TuriModelParser(ModelXmlFactory xmlFactory) {
        this.xmlFactory = xmlFactory;
    }

    /**
     * Locates a specific file named 'pom.turi' within the given directory.
     * If the file exists, it triggers the conversion of 'pom.turi' to 'pom.xml'
     * using the {@code turi2Xml} method and wraps the {@code pom.turi} file path
     * in an {@code Optional}. If the file does not exist, it returns an empty {@code Optional}.
     *
     * @param dir the directory in which to search for the 'pom.turi' file; must not be null
     * @return an {@code Optional} containing the located {@code Source} object if the 'pom.turi' file exists,
     *         or an empty {@code Optional} if the file is not found
     */
    @Override
    public Optional<Source> locate(Path dir) {
        final var path = dir.resolve("pom.turi");
        if (path.toFile().exists()) {
            turi2Xml(dir.toFile());
            return Optional.of(Sources.fromPath(path));
        }
        return Optional.empty();
    }


    @Override
    public Model parse(Source source, Map<String, ?> map) throws ModelParserException {
        final var pomFile = source.getPath();
        try (Reader reader = Files.newBufferedReader(pomFile)) {
            return xmlFactory.read(reader);
        } catch (Exception e) {
            throw new ModelParserException("Cannot parse the file " + source.getPath(), e);
        }
    }


}
