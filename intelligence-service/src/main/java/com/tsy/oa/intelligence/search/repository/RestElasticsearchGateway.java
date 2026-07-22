package com.tsy.oa.intelligence.search.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class RestElasticsearchGateway implements ElasticsearchGateway {

    private static final Pattern SAFE_PATH_SEGMENT = Pattern.compile("[a-z0-9][a-z0-9._-]*");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public RestElasticsearchGateway(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public boolean isAnalyzerAvailable(String analyzer) throws IOException {
        Request request = new Request("POST", "/_analyze");
        request.setJsonEntity(objectMapper.writeValueAsString(Map.of(
                "analyzer", analyzer,
                "text", "办公管理系统中文分词检查"
        )));
        try {
            restClient.performRequest(request);
            return true;
        } catch (ResponseException exception) {
            int status = exception.getResponse().getStatusLine().getStatusCode();
            if (status == 400 || status == 404) {
                return false;
            }
            throw exception;
        }
    }

    @Override
    public boolean indexExists(String indexName) throws IOException {
        Request request = new Request("HEAD", "/" + pathSegment(indexName));
        try {
            Response response = restClient.performRequest(request);
            return response.getStatusLine().getStatusCode() != 404;
        } catch (ResponseException exception) {
            if (exception.getResponse().getStatusLine().getStatusCode() == 404) {
                return false;
            }
            throw exception;
        }
    }

    @Override
    public void createIndex(String indexName, String definition) throws IOException {
        Request request = new Request("PUT", "/" + pathSegment(indexName));
        request.setJsonEntity(definition);
        requireAcknowledged(restClient.performRequest(request), "create index");
    }

    @Override
    public void updateMapping(String indexName, String mapping) throws IOException {
        Request request = new Request("PUT", "/" + pathSegment(indexName) + "/_mapping");
        request.setJsonEntity(mapping);
        requireAcknowledged(restClient.performRequest(request), "update mapping");
    }

    @Override
    public boolean fieldMappingMatches(
            String indexName,
            String fieldName,
            String fieldType
    ) throws IOException {
        Request request = new Request("GET", "/" + pathSegment(indexName) + "/_mapping");
        try {
            JsonNode root = objectMapper.readTree(EntityUtils.toString(
                    restClient.performRequest(request).getEntity()
            ));
            Iterator<JsonNode> mappings = root.elements();
            while (mappings.hasNext()) {
                JsonNode field = mappings.next().path("mappings").path("properties").path(fieldName);
                if (fieldType.equals(field.path("type").asText())) {
                    return true;
                }
            }
            return false;
        } catch (ResponseException exception) {
            if (exception.getResponse().getStatusLine().getStatusCode() == 404) {
                return false;
            }
            throw exception;
        }
    }

    @Override
    public String aliasTarget(String aliasName) throws IOException {
        Request request = new Request("GET", "/_alias/" + pathSegment(aliasName));
        try {
            JsonNode root = objectMapper.readTree(EntityUtils.toString(
                    restClient.performRequest(request).getEntity()
            ));
            Iterator<String> targets = root.fieldNames();
            return targets.hasNext() ? targets.next() : null;
        } catch (ResponseException exception) {
            if (exception.getResponse().getStatusLine().getStatusCode() == 404) {
                return null;
            }
            throw exception;
        }
    }

    @Override
    public void switchAlias(String aliasName, String targetIndex) throws IOException {
        String safeAlias = pathSegment(aliasName);
        String safeTarget = pathSegment(targetIndex);
        String currentTarget = aliasTarget(safeAlias);
        if (safeTarget.equals(currentTarget)) {
            return;
        }
        List<Map<String, Map<String, String>>> actions = new ArrayList<>();
        if (currentTarget != null) {
            actions.add(Map.of("remove", Map.of("index", currentTarget, "alias", safeAlias)));
        }
        actions.add(Map.of("add", Map.of("index", safeTarget, "alias", safeAlias)));
        Request request = new Request("POST", "/_aliases");
        request.setJsonEntity(objectMapper.writeValueAsString(Map.of("actions", actions)));
        requireAcknowledged(restClient.performRequest(request), "switch alias");
    }

    @Override
    public void upsertDocument(String indexName, String documentId, String source) throws IOException {
        Request request = new Request(
                "PUT",
                "/" + pathSegment(indexName) + "/_doc/" + pathSegment(documentId)
        );
        request.setJsonEntity(source);
        restClient.performRequest(request);
    }

    @Override
    public void deleteDocument(String indexName, String documentId) throws IOException {
        Request request = new Request(
                "DELETE",
                "/" + pathSegment(indexName) + "/_doc/" + pathSegment(documentId)
        );
        try {
            restClient.performRequest(request);
        } catch (ResponseException exception) {
            if (exception.getResponse().getStatusLine().getStatusCode() != 404) {
                throw exception;
            }
        }
    }

    @Override
    public String search(String indexName, String requestBody) throws IOException {
        Request request = new Request("POST", "/" + pathSegment(indexName) + "/_search");
        request.setJsonEntity(requestBody);
        return EntityUtils.toString(restClient.performRequest(request).getEntity());
    }

    @Override
    public String bulk(String requestBody) throws IOException {
        Request request = new Request("POST", "/_bulk");
        request.setJsonEntity(requestBody);
        return EntityUtils.toString(restClient.performRequest(request).getEntity());
    }

    @Override
    public void refreshIndex(String indexName) throws IOException {
        Request request = new Request("POST", "/" + pathSegment(indexName) + "/_refresh");
        JsonNode body = objectMapper.readTree(EntityUtils.toString(
                restClient.performRequest(request).getEntity()
        ));
        JsonNode failed = body.path("_shards").get("failed");
        if (failed == null || !failed.isIntegralNumber() || failed.asLong() > 0) {
            throw new IOException("Elasticsearch refresh did not complete on all shards");
        }
    }

    private void requireAcknowledged(Response response, String operation) throws IOException {
        JsonNode body = objectMapper.readTree(EntityUtils.toString(response.getEntity()));
        JsonNode acknowledged = body.get("acknowledged");
        if (acknowledged == null || !acknowledged.isBoolean() || !acknowledged.booleanValue()) {
            throw new IOException("Elasticsearch " + operation + " was not acknowledged");
        }
    }

    private String pathSegment(String value) {
        if (value == null || !SAFE_PATH_SEGMENT.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid Elasticsearch path segment");
        }
        return value;
    }
}
