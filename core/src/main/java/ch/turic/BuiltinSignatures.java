package ch.turic;

import ch.turic.analyzer.Lexer;
import ch.turic.analyzer.ParameterDefinition;
import ch.turic.commands.ParameterList;
import ch.turic.exceptions.ExecutionException;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Parses and caches Java builtin Turicum signatures declared through {@link TuriParameters}.
 */
final class BuiltinSignatures {
    private static final ConcurrentHashMap<Class<?>, Optional<ParameterList>> CACHE = new ConcurrentHashMap<>();

    private BuiltinSignatures() {
    }

    static ParameterList parametersFor(Object callable) {
        return CACHE.computeIfAbsent(callable.getClass(), BuiltinSignatures::parseAnnotatedParameters).orElse(null);
    }

    private static Optional<ParameterList> parseAnnotatedParameters(Class<?> klass) {
        final var annotation = klass.getAnnotation(TuriParameters.class);
        if (annotation == null) {
            return Optional.empty();
        }
        final var source = annotation.value();
        try {
            final var input = Input.fromString(source, "@TuriParameters(" + klass.getName() + ")");
            final var lexes = Lexer.analyze(input);
            final var parameters = ParameterDefinition.INSTANCE.analyze(lexes);
            if (!lexes.isEmpty()) {
                throw lexes.syntaxError("Extra token '%s' after builtin parameter declaration", lexes.peek().text());
            }
            return Optional.of(parameters);
        } catch (RuntimeException e) {
            throw new ExecutionException(e, "Cannot parse @TuriParameters on %s: %s", klass.getName(), source);
        }
    }
}
