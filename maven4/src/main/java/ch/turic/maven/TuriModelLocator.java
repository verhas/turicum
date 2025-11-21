package ch.turic.maven;

import org.apache.maven.api.services.ModelSource;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.nio.file.Path;

import static ch.turic.maven.FileDebugLogger.debug;

@Singleton
@Named
public class TuriModelLocator implements ModelSource.ModelLocator {

    @Inject
    public TuriModelLocator() {
    }

    @Override
    public Path locateExistingPom(Path project) {
        debug("Locating existing pom for project: " + project);
        debug("Returning null");
        return null;//project.resolve("pom.xml");
    }

    @PostConstruct
    public void initialize() {
    }
}
