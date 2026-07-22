package com.tsy.oa.intelligence.search.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ElasticsearchStubServer implements AutoCloseable {

    private final HttpServer server;
    private final Map<String, String> documents = new LinkedHashMap<>();
    private final Map<String, String> indexDefinitions = new LinkedHashMap<>();
    private final List<String> bulkRequests = new CopyOnWriteArrayList<>();
    private final List<String> deleteByQueryRequests = new CopyOnWriteArrayList<>();
    private final List<String> refreshRequests = new CopyOnWriteArrayList<>();
    private final List<String> operations = new CopyOnWriteArrayList<>();
    private final Deque<String> bulkResponses = new ArrayDeque<>();
    private final Deque<String> noticeSourceResponses = new ArrayDeque<>();
    private final Deque<String> applicationSourceResponses = new ArrayDeque<>();
    private final List<String> sourceRequests = new CopyOnWriteArrayList<>();
    private boolean analyzerAvailable = true;
    private String searchResponse = "{\"hits\":{\"total\":{\"value\":0,\"relation\":\"eq\"},\"hits\":[]}}";
    private String lastSearchRequestBody;

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

    public void setSearchResponse(String searchResponse) {
        this.searchResponse = searchResponse;
    }

    public String lastSearchRequestBody() {
        return lastSearchRequestBody;
    }

    public List<String> bulkRequests() {
        return List.copyOf(bulkRequests);
    }

    public List<String> deleteByQueryRequests() {
        return List.copyOf(deleteByQueryRequests);
    }

    public List<String> refreshRequests() {
        return List.copyOf(refreshRequests);
    }

    public List<String> operations() {
        return List.copyOf(operations);
    }

    public void setBulkResponses(String... responses) {
        bulkResponses.clear();
        bulkResponses.addAll(List.of(responses));
    }

    public void seedDocument(String indexName, String documentId, String source) {
        documents.put("/" + indexName + "/_doc/" + documentId, source);
    }

    public void reset() {
        documents.clear();
        bulkRequests.clear();
        deleteByQueryRequests.clear();
        refreshRequests.clear();
        operations.clear();
        bulkResponses.clear();
        noticeSourceResponses.clear();
        applicationSourceResponses.clear();
        sourceRequests.clear();
        searchResponse = "{\"hits\":{\"total\":{\"value\":0,\"relation\":\"eq\"},\"hits\":[]}}";
        lastSearchRequestBody = null;
    }

    public void setNoticeSourceResponses(String... responses) {
        noticeSourceResponses.clear();
        noticeSourceResponses.addAll(List.of(responses));
    }

    public void setApplicationSourceResponses(String... responses) {
        applicationSourceResponses.clear();
        applicationSourceResponses.addAll(List.of(responses));
    }

    public List<String> sourceRequests() {
        return List.copyOf(sourceRequests);
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


        if ("POST".equals(method) && path.endsWith("/_search")) {
            lastSearchRequestBody = requestBody;
            operations.add("search:" + path);
            respond(exchange, 200, searchResponse);
            return;
        }

        if ("POST".equals(method) && path.endsWith("/_delete_by_query")) {
            deleteByQueryRequests.add(path + "\n" + requestBody);
            operations.add("delete:" + path);
            String indexPrefix = path.substring(0, path.length() - "/_delete_by_query".length()) + "/_doc/";
            documents.keySet().removeIf(documentPath -> documentPath.startsWith(indexPrefix));
            respond(exchange, 200, "{\"deleted\":1,\"failures\":[]}");
            return;
        }

        if ("POST".equals(method) && path.endsWith("/_refresh")) {
            refreshRequests.add(path);
            operations.add("refresh:" + path);
            respond(exchange, 200, "{\"_shards\":{\"failed\":0}}");
            return;
        }

        if ("POST".equals(method) && "/_bulk".equals(path)) {
            bulkRequests.add(requestBody);
            operations.add("bulk");
            String response = bulkResponses.isEmpty()
                    ? "{\"errors\":false,\"items\":[]}"
                    : bulkResponses.removeFirst();
            respond(exchange, 200, response);
            return;
        }

        if ("GET".equals(method) && "/internal/notices/search-source".equals(path)) {
            sourceRequests.add(exchange.getRequestURI().toString());
            operations.add("source:notices");
            respond(exchange, 200, noticeSourceResponses.removeFirst());
            return;
        }

        if ("GET".equals(method) && "/internal/flow/search-source".equals(path)) {
            sourceRequests.add(exchange.getRequestURI().toString());
            operations.add("source:applications");
            respond(exchange, 200, applicationSourceResponses.removeFirst());
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
