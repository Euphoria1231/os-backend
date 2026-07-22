package com.tsy.oa.intelligence.search.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ElasticsearchStubServer implements AutoCloseable {

    private final HttpServer server;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> documents = new LinkedHashMap<>();
    private final Map<String, String> indexDefinitions = new LinkedHashMap<>();
    private final Map<String, Set<String>> mappedFields = new HashMap<>();
    private final Map<String, String> aliases = new HashMap<>();
    private final List<String> mappingRequests = new CopyOnWriteArrayList<>();
    private final List<String> aliasRequests = new CopyOnWriteArrayList<>();
    private final List<String> bulkRequests = new CopyOnWriteArrayList<>();
    private final List<String> deleteByQueryRequests = new CopyOnWriteArrayList<>();
    private final List<String> refreshRequests = new CopyOnWriteArrayList<>();
    private final List<String> operations = new CopyOnWriteArrayList<>();
    private final Deque<String> bulkResponses = new ArrayDeque<>();
    private final Deque<String> refreshResponses = new ArrayDeque<>();
    private final Deque<String> aliasResponses = new ArrayDeque<>();
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

    public List<String> mappingRequests() {
        return List.copyOf(mappingRequests);
    }

    public String aliasTarget(String alias) {
        return aliases.get(alias);
    }

    public List<String> aliasRequests() {
        return List.copyOf(aliasRequests);
    }

    public void seedIndexDefinition(String indexName, String definition) throws IOException {
        indexDefinitions.put(indexName, definition);
        mappedFields.put(indexName, readMappedFields(definition));
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

    public void setRefreshResponses(String... responses) {
        refreshResponses.clear();
        refreshResponses.addAll(List.of(responses));
    }

    public void setAliasResponses(String... responses) {
        aliasResponses.clear();
        aliasResponses.addAll(List.of(responses));
    }

    public void attachAlias(String alias, String indexName) {
        aliases.put(alias, indexName);
    }

    public void seedDocument(String indexName, String documentId, String source) {
        documents.put("/" + indexName + "/_doc/" + documentId, source);
    }

    public void reset() {
        documents.clear();
        indexDefinitions.clear();
        mappedFields.clear();
        aliases.clear();
        bulkRequests.clear();
        deleteByQueryRequests.clear();
        refreshRequests.clear();
        operations.clear();
        bulkResponses.clear();
        refreshResponses.clear();
        aliasResponses.clear();
        noticeSourceResponses.clear();
        applicationSourceResponses.clear();
        sourceRequests.clear();
        mappingRequests.clear();
        aliasRequests.clear();
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
            String response = refreshResponses.isEmpty()
                    ? "{\"_shards\":{\"failed\":0}}"
                    : refreshResponses.removeFirst();
            respond(exchange, 200, response);
            return;
        }

        if ("POST".equals(method) && "/_bulk".equals(path)) {
            bulkRequests.add(requestBody);
            operations.add("bulk");
            String response = bulkResponses.isEmpty()
                    ? successfulBulkResponse(requestBody)
                    : bulkResponses.removeFirst();
            JsonNode errors = objectMapper.readTree(response).get("errors");
            if (errors != null && errors.isBoolean() && !errors.booleanValue()) {
                applyBulk(requestBody);
            }
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

        if ("GET".equals(method) && path.startsWith("/_alias/")) {
            String alias = path.substring("/_alias/".length());
            String target = aliases.get(alias);
            if (target == null) {
                respond(exchange, 404, "{\"error\":\"alias not found\"}");
            } else {
                respond(exchange, 200, "{\"" + target + "\":{\"aliases\":{\"" + alias + "\":{}}}}");
            }
            return;
        }

        if ("POST".equals(method) && "/_aliases".equals(path)) {
            aliasRequests.add(requestBody);
            String response = aliasResponses.isEmpty()
                    ? "{\"acknowledged\":true}"
                    : aliasResponses.removeFirst();
            if (objectMapper.readTree(response).path("acknowledged").asBoolean()) {
                for (JsonNode action : objectMapper.readTree(requestBody).path("actions")) {
                    JsonNode add = action.path("add");
                    if (!add.isMissingNode()) {
                        aliases.put(add.path("alias").asText(), add.path("index").asText());
                    }
                    JsonNode remove = action.path("remove");
                    if (!remove.isMissingNode()) {
                        aliases.remove(remove.path("alias").asText());
                    }
                }
            }
            respond(exchange, 200, response);
            return;
        }

        if (path.endsWith("/_mapping")) {
            String indexName = resolveIndex(path.substring(1, path.length() - "/_mapping".length()));
            if ("GET".equals(method)) {
                if (!indexDefinitions.containsKey(indexName)) {
                    respond(exchange, 404, "{\"error\":\"index not found\"}");
                } else {
                    String properties = mappedProperties(indexName);
                    respond(exchange, 200, "{\"" + indexName + "\":{\"mappings\":{\"properties\":" + properties + "}}}");
                }
                return;
            }
            if ("PUT".equals(method)) {
                mappingRequests.add(indexName + "\n" + requestBody);
                mappedFields.computeIfAbsent(indexName, ignored -> new HashSet<>())
                        .addAll(readMappedFields(requestBody));
                respond(exchange, 200, "{\"acknowledged\":true}");
                return;
            }
        }

        String indexName = path.substring(1);
        if ("HEAD".equals(method)) {
            respond(exchange, indexDefinitions.containsKey(indexName) || aliases.containsKey(indexName) ? 200 : 404, "");
            return;
        }
        if ("PUT".equals(method)) {
            indexDefinitions.put(indexName, requestBody);
            mappedFields.put(indexName, readMappedFields(requestBody));
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
            int marker = path.indexOf("/_doc/");
            String requestedIndex = path.substring(1, marker);
            String targetIndex = resolveIndex(requestedIndex);
            if (mappedFields.containsKey(targetIndex)
                    && !mappedFields.get(targetIndex).containsAll(readDocumentFields(requestBody))) {
                respond(exchange, 400, "{\"error\":\"strict_dynamic_mapping_exception\"}");
                return;
            }
            String targetPath = "/" + targetIndex + path.substring(marker);
            boolean existed = documents.put(targetPath, requestBody) != null;
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

    private String resolveIndex(String indexOrAlias) {
        return aliases.getOrDefault(indexOrAlias, indexOrAlias);
    }

    private Set<String> readMappedFields(String json) throws IOException {
        JsonNode root = objectMapper.readTree(json);
        JsonNode properties = root.path("mappings").path("properties");
        if (properties.isMissingNode()) {
            properties = root.path("properties");
        }
        Set<String> fields = new HashSet<>();
        properties.fieldNames().forEachRemaining(fields::add);
        return fields;
    }

    private Set<String> readDocumentFields(String json) throws IOException {
        Set<String> fields = new HashSet<>();
        objectMapper.readTree(json).fieldNames().forEachRemaining(fields::add);
        return fields;
    }

    private String mappedProperties(String indexName) throws IOException {
        com.fasterxml.jackson.databind.node.ObjectNode properties = objectMapper.createObjectNode();
        for (String field : mappedFields.getOrDefault(indexName, Set.of())) {
            properties.putObject(field).put("type", "approverId".equals(field) ? "long" : "keyword");
        }
        return objectMapper.writeValueAsString(properties);
    }

    private String successfulBulkResponse(String requestBody) throws IOException {
        String[] lines = requestBody.strip().split("\\n");
        com.fasterxml.jackson.databind.node.ArrayNode items = objectMapper.createArrayNode();
        for (int index = 0; index < lines.length; index += 2) {
            String id = objectMapper.readTree(lines[index]).path("index").path("_id").asText();
            items.addObject().putObject("index").put("_id", id).put("status", 201);
        }
        com.fasterxml.jackson.databind.node.ObjectNode response = objectMapper.createObjectNode();
        response.put("errors", false);
        response.set("items", items);
        return objectMapper.writeValueAsString(response);
    }

    private void applyBulk(String requestBody) throws IOException {
        String[] lines = requestBody.strip().split("\\n");
        for (int index = 0; index < lines.length; index += 2) {
            JsonNode action = objectMapper.readTree(lines[index]).path("index");
            String target = resolveIndex(action.path("_index").asText());
            String id = action.path("_id").asText();
            String source = lines[index + 1];
            if (mappedFields.containsKey(target)
                    && !mappedFields.get(target).containsAll(readDocumentFields(source))) {
                continue;
            }
            documents.put("/" + target + "/_doc/" + id, source);
        }
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
