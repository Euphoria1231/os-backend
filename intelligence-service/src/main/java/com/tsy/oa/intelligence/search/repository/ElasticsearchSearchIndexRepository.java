package com.tsy.oa.intelligence.search.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsy.oa.intelligence.search.config.ElasticsearchSearchProperties;
import com.tsy.oa.intelligence.search.model.ApplicationSearchDocument;
import com.tsy.oa.intelligence.search.model.NoticeSearchDocument;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.Objects;

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
                properties.getNoticeIndex(),
                NOTICE_ID_PREFIX + document.noticeId(),
                objectMapper.writeValueAsString(document)
        );
    }

    @Override
    public void deleteNotice(long noticeId) throws IOException {
        gateway.deleteDocument(properties.getNoticeIndex(), NOTICE_ID_PREFIX + noticeId);
    }

    @Override
    public void saveApplication(ApplicationSearchDocument document) throws IOException {
        Objects.requireNonNull(document, "document must not be null");
        gateway.upsertDocument(
                properties.getApplicationIndex(),
                APPLICATION_ID_PREFIX + document.applicationId(),
                objectMapper.writeValueAsString(document)
        );
    }

    @Override
    public void deleteApplication(long applicationId) throws IOException {
        gateway.deleteDocument(properties.getApplicationIndex(), APPLICATION_ID_PREFIX + applicationId);
    }
}
