package com.tsy.oa.intelligence.search.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
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
        restClient.performRequest(request);
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

    private String pathSegment(String value) {
        if (value == null || !SAFE_PATH_SEGMENT.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid Elasticsearch path segment");
        }
        return value;
    }
}
