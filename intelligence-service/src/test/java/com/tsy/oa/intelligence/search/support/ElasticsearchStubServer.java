package com.tsy.oa.intelligence.search.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ElasticsearchStubServer implements AutoCloseable {

    private final HttpServer server;
    private final Map<String, String> documents = new LinkedHashMap<>();
    private final Map<String, String> indexDefinitions = new LinkedHashMap<>();
    private boolean analyzerAvailable = true;

    public ElasticsearchStubServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    public String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public Map<String, String> documents() {
        return Map.copyOf(documents);
    }

    public Map<String, String> indexDefinitions() {
        return Map.copyOf(indexDefinitions);
    }

    public void setAnalyzerAvailable(boolean analyzerAvailable) {
        this.analyzerAvailable = analyzerAvailable;
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

        if ("POST".equals(method) && "/_analyze".equals(path)) {
            if (analyzerAvailable) {
                respond(exchange, 200, "{\"tokens\":[]}");
            } else {
                respond(exchange, 400, "{\"error\":\"unknown analyzer\"}");
            }
            return;
        }

        if (path.contains("/_doc/")) {
            handleDocument(exchange, method, path, requestBody);
            return;
        }

        String indexName = path.substring(1);
        if ("HEAD".equals(method)) {
            respond(exchange, indexDefinitions.containsKey(indexName) ? 200 : 404, "");
            return;
        }
        if ("PUT".equals(method)) {
            indexDefinitions.put(indexName, requestBody);
            respond(exchange, 200, "{\"acknowledged\":true}");
            return;
        }

        respond(exchange, 404, "{\"error\":\"not found\"}");
    }

    private void handleDocument(
            HttpExchange exchange,
            String method,
            String path,
            String requestBody
    ) throws IOException {
        if ("PUT".equals(method)) {
            boolean existed = documents.put(path, requestBody) != null;
            respond(exchange, existed ? 200 : 201, "{\"result\":\"" + (existed ? "updated" : "created") + "\"}");
            return;
        }
        if ("DELETE".equals(method)) {
            boolean existed = documents.remove(path) != null;
            respond(exchange, existed ? 200 : 404, "{\"result\":\"" + (existed ? "deleted" : "not_found") + "\"}");
            return;
        }
        respond(exchange, 405, "{\"error\":\"method not allowed\"}");
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        if ("HEAD".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
            return;
        }

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
