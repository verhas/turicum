package ch.turic.maven;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.maven.api.services.ModelSource;

import javax.annotation.PostConstruct;
import java.nio.file.Path;

@Singleton
@Named("turicum-maven-extension")
public class CustomModelLocator implements ModelSource.ModelLocator {

    @Inject
    public CustomModelLocator() {
    }

    static {
        System.out.println("CustomModelLocator loaded, not created yeti.");
    }

    @Override
    public Path locateExistingPom(Path project) {
        return project.resolve("pom.xml");
    }

    @PostConstruct
    public void initialize() {
        // Generate POM here
        System.out.println("CustomModelLocator initialized");
    }
}
