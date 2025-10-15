package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.SnakeNamed.Name;
import ch.turic.TuriFunction;
import ch.turic.builtins.classes.TuriMethod;
import ch.turic.memory.LngList;
import ch.turic.memory.LngObject;
import ch.turic.memory.LocalContext;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
/*snippet builtin0470

=== `http_client`

end snippet */

/**
 * Perform a http request to a remote (or not so remote) server.
 */
@Name("http_client")
public class TuriHttpClient implements TuriFunction {

    private static String str(Object s) {
        if (s == null) {
            return null;
        }
        return s.toString();
    }

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        final var parameters = FunUtils.arg(name(), arguments, LngObject.class);

        final var streamMode = parameters.getField("stream", true);

        final var request = buildRequest(parameters);

        try {
            final var client = buildClient(parameters);
            HttpResponse<?> response = streamMode ?
                    client.send(request, HttpResponse.BodyHandlers.ofInputStream()) :
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            final var result = LngObject.newEmpty(ctx);
            final var inputStream = (InputStream) (streamMode ? response.body() : null);
            final var inputStreamReader = streamMode ? new InputStreamReader(inputStream) : null;

            TuriMethod<Object> resourceCloser = new TuriMethod<>((args) -> {
                if (inputStream != null) {
                    inputStream.readAllBytes();
                }
                client.close();
                return null;
            });

            result.setField("status", response.statusCode());
            result.setField("body", streamMode ? null : response.body());
            result.setField("input", inputStream);
            result.setField("stream", inputStreamReader);
            result.setField("uri", str(response.uri()));
            setHeaders(ctx, result, response);
            result.setField("version", response.version().toString());
            result.setField("close", resourceCloser);
            result.setField("exit", resourceCloser);
            result.setField("entry", new TuriMethod<>((args) -> result));
            return result;
        } catch (Exception e) {
            throw new ExecutionException("HTTP request failed: " + e.getMessage());
        }
    }

    /**
     * Sets the headers from the given HTTP response into the result object.
     *
     * @param context  the execution context to be used for creating the header structure
     * @param result   the LngObject where the headers will be set
     * @param response the HTTP response from which the headers will be extracted
     */
    private void setHeaders(LocalContext context, LngObject result, HttpResponse<?> response) {
        final var headers = LngObject.newEmpty(context);
        for (final var header : response.headers().map().entrySet()) {
            final var headerValues = new LngList();
            headerValues.addAll(header.getValue());
            headers.setField(header.getKey(), headerValues);
        }
        result.setField("headers", headers);
    }

    private HttpClient buildClient(LngObject parameters) {
        final var builder = HttpClient.newBuilder();

        final var redirect = str(parameters.getField("redirect_policy"));
        if (redirect != null) {
            try {
                builder.followRedirects(HttpClient.Redirect.valueOf(redirect));
            } catch (IllegalArgumentException e) {
                throw new ExecutionException("Invalid redirect policy: " + redirect);
            }
        }

        final var timeout = parameters.getField("timeout");
        if (timeout != null) {
            if (timeout instanceof Duration dur) {
                builder.connectTimeout(dur);
            } else {
                throw new ExecutionException("Invalid timeout value: " + timeout);
            }
        }

        return builder.build();
    }

    private HttpRequest buildRequest(LngObject parameters) {
        final var builder = HttpRequest.newBuilder();

        final var method = str(parameters.getField("method", "GET"));
        final var validMethods = Set.of("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS");
        if (!validMethods.contains(method.toUpperCase())) {
            throw new ExecutionException("Invalid HTTP method: " + method);
        }
        final var body = str(parameters.getField("body"));
        final HttpRequest.BodyPublisher bodyPublisher;
        if (body == null) {
            bodyPublisher = HttpRequest.BodyPublishers.noBody();
        } else {
            bodyPublisher = HttpRequest.BodyPublishers.ofString(body);
        }

        final var headers = parameters.getField("headers");
        if (headers != null) {
            if (headers instanceof LngObject lngHeaders) {
                for (final var header : lngHeaders.fields()) {
                    if (lngHeaders.getField(header) instanceof LngList lngList) {
                        for (final var h : lngList.array) {
                            builder.header(header, str(h));
                        }
                    } else {
                        builder.header(header, str(lngHeaders.getField(header)));
                    }
                }
            } else {
                throw new ExecutionException("Field 'headers' has to be an object, it is '%s'.", headers);
            }
        }

        builder.method(method, bodyPublisher);

        final var url = str(parameters.getField("url"));
        if (url == null) {
            throw new ExecutionException("Missing 'url' parameter");
        }
        try {
            builder.uri(URI.create(url));
        } catch (IllegalArgumentException e) {
            throw new ExecutionException("Invalid URL: " + url);
        }
        return builder.build();
    }
}
