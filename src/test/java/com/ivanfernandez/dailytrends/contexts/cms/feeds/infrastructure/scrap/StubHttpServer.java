package com.ivanfernandez.dailytrends.contexts.cms.feeds.infrastructure.scrap;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

/** A local HTTP server with scripted responses that records the request headers it receives. */
final class StubHttpServer implements AutoCloseable {

    private final HttpServer server;
    private final List<Headers> requests = new ArrayList<>();
    private final CountDownLatch closing = new CountDownLatch(1);

    private StubHttpServer(HttpServer server) {
        this.server = server;
    }

    static StubHttpServer start() {
        try {
            var server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            server.start();
            return new StubHttpServer(server);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    String url(String path) {
        return "http://localhost:" + server.getAddress().getPort() + path;
    }

    void respond(String path, int status, String contentType, byte[] body) {
        on(path, exchange -> {
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
    }

    void redirect(String path, String location) {
        on(path, exchange -> {
            exchange.getResponseHeaders().set("Location", location);
            exchange.sendResponseHeaders(301, -1);
            exchange.close();
        });
    }

    /** Never answers: the request hangs until the server is closed. */
    void hang(String path) {
        on(path, exchange -> {
            try {
                closing.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            exchange.close();
        });
    }

    /** Sends the headers at once, then each chunk after a pause. */
    void respondInChunks(String path, String contentType, List<byte[]> chunks, Duration pause) {
        on(path, exchange -> {
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream out = exchange.getResponseBody()) {
                for (byte[] chunk : chunks) {
                    out.write(chunk);
                    out.flush();
                    // The stub plays a slow server; the test itself never sleeps.
                    Thread.sleep(pause);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (IOException exception) {
                // The client gave up: expected when it times out.
            }
        });
    }

    synchronized List<Headers> requests() {
        return List.copyOf(requests);
    }

    @Override
    public void close() {
        closing.countDown();
        server.stop(0);
    }

    private void on(String path, HttpHandler handler) {
        server.createContext(path, exchange -> {
            record(exchange);
            handler.handle(exchange);
        });
    }

    private synchronized void record(HttpExchange exchange) {
        requests.add(exchange.getRequestHeaders());
    }
}
