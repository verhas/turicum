package ch.turic.maven;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.Source;
import org.apache.maven.api.spi.ModelParser;
import org.apache.maven.api.spi.ModelParserException;
import org.apache.maven.model.io.DefaultModelReader;

import static ch.turic.maven.CustomModelProcessor.turi2Xml;

@Singleton
@Named("turicum-maven-extension")
public class CustomModelParser implements ModelParser {
    @Override
    public Optional<Source> locate(Path dir) {
        final var path = dir.resolve("pom.turi");
        if (path.toFile().exists()) {
            turi2Xml(dir.toFile());
            return Optional.of(Source.fromPath(path));
        }
        return Optional.empty();
    }

    @Override
    public Model parse(Source source, Map<String, ?> map) throws ModelParserException {
        try {
            final var pomFile = source.getPath().toFile();
            final var reader = new DefaultModelReader(null);
            Map<String, Object> options = new HashMap<>();
            if (map != null) {
                options.putAll(map);
            }

            try (FileInputStream fis = new FileInputStream(pomFile)) {
                return reader.read(fis, options).getDelegate();
            }
        } catch (Exception e) {
            throw new ModelParserException("Cannot parse the file " + source.getPath(), e);
        }
    }


}
