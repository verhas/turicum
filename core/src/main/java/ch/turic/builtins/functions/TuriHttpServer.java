package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.Closure;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;

/**
 * Start an HTTP server
 */
public class TuriHttpServer implements TuriFunction {
    @Override
    public String name() {
        return "server";
    }

    static class LimitedVirtualThreadExecutor implements Executor {
        private final Semaphore semaphore;

        public LimitedVirtualThreadExecutor(int maxConcurrent) {
            this.semaphore = new Semaphore(maxConcurrent);
        }

        @Override
        public void execute(Runnable command) {
            try {
                semaphore.acquire(); // limit concurrency
                Thread.startVirtualThread(() -> {
                    try {
                        command.run();
                    } finally {
                        semaphore.release();
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RejectedExecutionException("Interrupted while acquiring semaphore", e);
            }
        }
    }

    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        FunUtils.oneArg(name(), args);
        if (!(args[0] instanceof LngObject conf)) {
            throw new ExecutionException("%s needs an object as configuration argument", name());
        }

        final var port = Cast.toLong(conf.getField("port")).intValue();
        final var host = conf.getField("host").toString();
        final HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(host, port), 0);
        } catch (Exception e) {
            throw new ExecutionException(e);
        }

        final var concurrency = Cast.toLong(Objects.requireNonNullElse(conf.getField("concurrency"),10)).intValue();
        final var executor = new LimitedVirtualThreadExecutor(concurrency);

        final ChannelIterator<?> channel = (ChannelIterator<?>) new BlockingQueueChannel<>(concurrency).iterator();

        final var routes = conf.getField("routes");
        if (!(routes instanceof Iterable<?> r)) {
            throw new ExecutionException("%s needs a list as routes as configuration argument", name());
        }
        for (final var rr : r) {
            if (!(rr instanceof LngObject route)) {
                throw new ExecutionException("%s got '%s' as route, this is not an object", name(), rr);
            }
            final var path = route.getField("path").toString();
            final var handler = route.getField("handler");
            if (!(handler instanceof Closure closure)) {
                throw new ExecutionException("Handler is '%s' not a closure or function", handler);
            }
            server.createContext(path, (exchange) -> {
                        final var handlerCtx = ctx.thread();
                        final var request = mapRequest(exchange, handlerCtx);
                        final var requestMessage = mapRequest(exchange, handlerCtx);
                        final var response = mapResponse(exchange, handlerCtx);
                        try {
                            channel.send((Channel.Message) Channel.Message.of(requestMessage));
                            final var result = closure.call(handlerCtx.open(), request, response).toString();

                            exchange.sendResponseHeaders(Cast.toLong(response.getField("code")).intValue(), result.length());
                            try (final var out = exchange.getResponseBody()) {
                                out.write(result.getBytes(StandardCharsets.UTF_8));
                                out.flush();
                            }
                        }catch(Exception e) {
                            final var exception = Channel.Message.exception(LngException.build(handlerCtx,e,handlerCtx.threadContext.getStackTrace()));
                            channel.send((Channel.Message) exception);
                        }
                    }
            );
        }


        server.setExecutor(executor);
        server.start();
        return channel;
    }

    private LngObject mapResponse(HttpExchange exchange, ch.turic.memory.Context ctx) {
        final var response = new LngObject(null, ctx.open());
        response.setField("headers", exchange.getResponseHeaders());
        response.setField("code", exchange.getResponseCode());
        return response;
    }

    private LngObject mapRequest(HttpExchange exchange, ch.turic.memory.Context ctx) {
        final var request = new LngObject(null, ctx.open());
        request.setField("method", exchange.getRequestMethod());
        final var client = new LngObject(null, ctx.open());
        client.setField("host", exchange.getRemoteAddress().getAddress().getHostAddress());
        client.setField("port", exchange.getRemoteAddress().getPort());
        request.setField("client", client);
        request.setField("protocol", exchange.getProtocol());
        final var srv = new LngObject(null, ctx.open());
        srv.setField("host", exchange.getLocalAddress().getAddress().getHostAddress());
        srv.setField("port", exchange.getLocalAddress().getPort());
        request.setField("server", srv);
        final var headers = new LngObject(null, ctx.open());
        for( final var h : exchange.getRequestHeaders().entrySet() ) {
            final var headerValues = new LngList();
            headerValues.array.addAll(h.getValue());
            headers.setField(h.getKey(), headerValues);
        }
        request.setField("headers", headers);
        request.setField("uri", exchange.getRequestURI().toString());
        request.setField("body", exchange.getRequestBody());
        return request;
    }
}
