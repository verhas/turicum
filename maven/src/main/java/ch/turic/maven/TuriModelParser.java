package ch.turic.maven;

import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.Source;
import org.apache.maven.api.services.Sources;
import org.apache.maven.api.services.xml.ModelXmlFactory;
import org.apache.maven.api.spi.ModelParser;
import org.apache.maven.api.spi.ModelParserException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static ch.turic.maven.TuriModelProcessor.turi2Xml;

@Singleton
@Named("turicum-maven-extension")
public class TuriModelParser implements ModelParser {

    @Inject
    private final ModelXmlFactory xmlFactory;

    public TuriModelParser(ModelXmlFactory xmlFactory) {
        this.xmlFactory = xmlFactory;
    }

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
