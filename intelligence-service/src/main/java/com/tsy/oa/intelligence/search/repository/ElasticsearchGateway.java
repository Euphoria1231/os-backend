package com.tsy.oa.intelligence.search.repository;

import java.io.IOException;

public interface ElasticsearchGateway {

    boolean isAnalyzerAvailable(String analyzer) throws IOException;

    boolean indexExists(String indexName) throws IOException;

    void createIndex(String indexName, String definition) throws IOException;

    void upsertDocument(String indexName, String documentId, String source) throws IOException;

    void deleteDocument(String indexName, String documentId) throws IOException;
}
