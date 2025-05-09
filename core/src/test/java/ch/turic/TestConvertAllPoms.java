package ch.turic;

import ch.turic.analyzer.Input;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

public class TestConvertAllPoms {

    /**
     * Convert all the pom.turi files to pom.xml running individual interpreter instances.
     *
     * @throws Exception if some of the files cannot be converted
     */
    @Test
    void convertAllPoms() throws Exception {
        Path projectRoot = Path.of("..").toRealPath();
        System.setProperty("APPIA",projectRoot.toAbsolutePath().toString());
        try (Stream<Path> stream = Files.walk(projectRoot)) {
            stream
                    .filter(path -> path.getFileName().toString().equals("pom.turi"))
                    .forEach(pomTuri -> {
                        try {
                            Interpreter interpreter = new Interpreter(Input.fromFile(pomTuri));
                            String pomXml = interpreter.execute().toString();
                            Path pomXmlPath = pomTuri.getParent().resolve("pom.xml");
                            Files.writeString(
                                    pomXmlPath,
                                    pomXml,
                                    StandardCharsets.UTF_8,
                                    StandardOpenOption.CREATE,
                                    StandardOpenOption.TRUNCATE_EXISTING
                            );
                            System.out.println("Converted: " + pomTuri + " -> " + pomXmlPath);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to convert: " + pomTuri, e);
                        }
                    });
        }
    }


}

