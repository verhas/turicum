package ch.turic.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TestMakeBuildTimeResource {
    @Test
    public void testMakeBuildTimeResource() throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        File resourcesDir = new File("src/main/resources");
        resourcesDir.mkdirs();

        File buildTimeFile = new File(resourcesDir, "buildtime.txt");
        try (FileWriter writer = new FileWriter(buildTimeFile)) {
            writer.write(timestamp);
        }

        Assertions.assertTrue(buildTimeFile.exists());
        Assertions.assertTrue(buildTimeFile.length() > 0);
    }
}
