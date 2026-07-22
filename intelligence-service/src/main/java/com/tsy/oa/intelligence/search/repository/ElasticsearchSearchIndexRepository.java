package com.tsy.oa.intelligence.search.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsy.oa.intelligence.search.config.ElasticsearchSearchProperties;
import com.tsy.oa.intelligence.search.model.ApplicationSearchDocument;
import com.tsy.oa.intelligence.search.model.NoticeSearchDocument;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.Objects;
import java.util.List;

@Repository
public class ElasticsearchSearchIndexRepository implements SearchIndexRepository {

    private static final String NOTICE_ID_PREFIX = "notice-";
    private static final String APPLICATION_ID_PREFIX = "application-";

    private final ElasticsearchGateway gateway;
    private final ObjectMapper objectMapper;
    private final ElasticsearchSearchProperties properties;

    public ElasticsearchSearchIndexRepository(
            ElasticsearchGateway gateway,
            ObjectMapper objectMapper,
            ElasticsearchSearchProperties properties
    ) {
        this.gateway = gateway;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public void saveNotice(NoticeSearchDocument document) throws IOException {
        Objects.requireNonNull(document, "document must not be null");
        gateway.upsertDocument(
                properties.getNoticeAlias(),
                NOTICE_ID_PREFIX + document.noticeId(),
                objectMapper.writeValueAsString(document)
        );
    }

    @Override
    public void deleteNotice(long noticeId) throws IOException {
        gateway.deleteDocument(properties.getNoticeAlias(), NOTICE_ID_PREFIX + noticeId);
    }

    @Override
    public void saveApplication(ApplicationSearchDocument document) throws IOException {
        Objects.requireNonNull(document, "document must not be null");
        gateway.upsertDocument(
                properties.getApplicationAlias(),
                APPLICATION_ID_PREFIX + document.applicationId(),
                objectMapper.writeValueAsString(document)
        );
    }

    @Override
    public void deleteApplication(long applicationId) throws IOException {
        gateway.deleteDocument(properties.getApplicationAlias(), APPLICATION_ID_PREFIX + applicationId);
    }

    @Override
    public void saveNoticeToIndex(String indexName, NoticeSearchDocument document) throws IOException {
        Objects.requireNonNull(document, "document must not be null");
        gateway.upsertDocument(
                indexName,
                NOTICE_ID_PREFIX + document.noticeId(),
                objectMapper.writeValueAsString(document)
        );
    }

    @Override
    public void deleteNoticeFromIndex(String indexName, long noticeId) throws IOException {
        gateway.deleteDocument(indexName, NOTICE_ID_PREFIX + noticeId);
    }

    @Override
    public void saveApplicationToIndex(
            String indexName,
            ApplicationSearchDocument document
    ) throws IOException {
        Objects.requireNonNull(document, "document must not be null");
        gateway.upsertDocument(
                indexName,
                APPLICATION_ID_PREFIX + document.applicationId(),
                objectMapper.writeValueAsString(document)
        );
    }

    @Override
    public void deleteApplicationFromIndex(String indexName, long applicationId) throws IOException {
        gateway.deleteDocument(indexName, APPLICATION_ID_PREFIX + applicationId);
    }

    @Override
    public void saveNotices(List<NoticeSearchDocument> documents) throws IOException {
        bulkSave(properties.getNoticeAlias(), NOTICE_ID_PREFIX, documents, NoticeSearchDocument::noticeId);
    }

    @Override
    public void saveNoticesToIndex(
            String indexName,
            List<NoticeSearchDocument> documents
    ) throws IOException {
        bulkSave(indexName, NOTICE_ID_PREFIX, documents, NoticeSearchDocument::noticeId);
    }

    @Override
    public void saveApplications(List<ApplicationSearchDocument> documents) throws IOException {
        bulkSave(
                properties.getApplicationAlias(), APPLICATION_ID_PREFIX,
                documents, ApplicationSearchDocument::applicationId
        );
    }

    @Override
    public void saveApplicationsToIndex(
            String indexName,
            List<ApplicationSearchDocument> documents
    ) throws IOException {
        bulkSave(indexName, APPLICATION_ID_PREFIX, documents, ApplicationSearchDocument::applicationId);
    }

    private <T> void bulkSave(
            String indexName,
            String idPrefix,
            List<T> documents,
            java.util.function.ToLongFunction<T> idExtractor
    ) throws IOException {
        Objects.requireNonNull(documents, "documents must not be null");
        if (documents.isEmpty()) {
            return;
        }
        StringBuilder requestBody = new StringBuilder();
        for (T document : documents) {
            Objects.requireNonNull(document, "document must not be null");
            requestBody.append(objectMapper.writeValueAsString(java.util.Map.of(
                    "index", java.util.Map.of(
                            "_index", indexName,
                            "_id", idPrefix + idExtractor.applyAsLong(document)
                    )
            ))).append('\n');
            requestBody.append(objectMapper.writeValueAsString(document)).append('\n');
        }
        String response = gateway.bulk(requestBody.toString());
        JsonNode body = objectMapper.readTree(response);
        JsonNode errors = body.get("errors");
        if (errors == null || !errors.isBoolean()) {
            throw new IOException("Elasticsearch bulk response has no boolean errors field");
        }
        JsonNode items = body.get("items");
        if (items == null || !items.isArray() || items.size() != documents.size()) {
            throw new IOException("Elasticsearch bulk response item count does not match request");
        }
        long failedItems = 0;
        for (JsonNode item : items) {
            JsonNode result = item.path("index");
            int status = result.path("status").asInt(500);
            if (status < 200 || status >= 300 || result.has("error")) {
                failedItems++;
            }
        }
        if (errors.booleanValue() || failedItems > 0) {
            throw new IOException("Elasticsearch bulk request contains " + failedItems + " failed items");
        }
    }
}
