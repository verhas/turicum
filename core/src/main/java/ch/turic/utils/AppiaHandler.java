package ch.turic.utils;

import ch.turic.ExecutionException;
import ch.turic.memory.LngList;
import ch.turic.memory.LocalContext;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class AppiaHandler {
    public static final String APPIA = "APPIA";
    private final List<Path> appiaRoots = getAppiaRoots();

    public static List<Path> getAppiaRoots() {
        var appia = System.getProperty(APPIA);
        if (appia == null) {
            System.getenv(APPIA);
        }
        if (appia == null) {
            appia = loadFromEnvFile();
        }
        if (appia == null || appia.isBlank()) {
            return List.of();
        }

        return toPathList(Arrays.stream(appia.split("\\|")));
    }

    private static String loadFromEnvFile() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            try {
                Path envPath = current.resolve(".env");
                if (Files.exists(envPath) && Files.isReadable(envPath)) {
                    var lines = Files.readAllLines(envPath, StandardCharsets.UTF_8);
                    for (var line : lines) {
                        var trimmed = line.trim();
                        if (trimmed.startsWith(APPIA + "=")) {
                            return trimmed.substring((APPIA + "=").length()).trim();
                        }
                    }
                }
            } catch (IOException ignored) {
            } finally {
                current = current.getParent();
            }
        }
        return null;
    }

    public static List<Path> toPathList(Stream<String> stringStream) {
        return stringStream.map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Path::of)
                .map(Path::normalize)
                .map(Path::toAbsolutePath)
                .toList();

    }

    public Path locateJar(LocalContext context, String arg) {
        final var relativePath = Path.of(arg);
        return locateSource(context, arg, relativePath);
    }

    public Path locateSource(LocalContext context, String arg) {
        final var relativePath = Path.of(arg.replace('.', File.separatorChar) + ".turi");
        return locateSource(context, arg, relativePath);
    }

    private Path locateSource(LocalContext context, String arg, Path relativePath) {
        final List<Path> appiaRoots;
        final String appiaSource; // used to error reporting only
        if (context.contains(APPIA)) {
            final var appia = context.get(APPIA);
            if (appia instanceof LngList appiaList) {
                appiaRoots = getAppiaRootsFrom(appiaList);
                appiaSource = "global variable";
            } else {
                throw new ExecutionException("There is an APPIA variable defined, but it is not a list. APPIA=%s", Objects.requireNonNullElse(appia, "none").toString());
            }
        } else {
            appiaRoots = this.appiaRoots;
            appiaSource = "environment variable";
        }
        Path sourceFile = null;
        for (var root : appiaRoots) {
            var candidate = root.resolve(relativePath);
            if (Files.exists(candidate)) {
                sourceFile = candidate;
                break;
            }
        }

        if (sourceFile == null) {
            throw new ExecutionException("There is no import '%s' via APPIA(%s)=[%s], cwd=%s",
                    arg,
                    appiaSource,
                    String.join("|", appiaRoots.stream().map(Path::toString).toList()),
                    new File(".").getAbsolutePath());
        }
        return sourceFile;
    }

    private List<Path> getAppiaRootsFrom(LngList appiaList) {
        final List<Path> appiaRoots;
        appiaRoots = toPathList(appiaList.array.stream()
                .map(o -> Objects.requireNonNullElse(o, "none"))
                .map(Object::toString));
        return appiaRoots;
    }
}
