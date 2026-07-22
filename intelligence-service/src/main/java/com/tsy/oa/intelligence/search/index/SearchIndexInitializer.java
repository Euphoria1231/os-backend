package com.tsy.oa.intelligence.search.index;

import com.tsy.oa.intelligence.search.config.ElasticsearchSearchProperties;
import com.tsy.oa.intelligence.search.repository.ElasticsearchGateway;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@ConditionalOnProperty(
        prefix = "oa.search.elasticsearch",
        name = "initialize",
        havingValue = "true",
        matchIfMissing = true
)
public class SearchIndexInitializer implements ApplicationRunner {

    private static final String ELASTICSEARCH_VERSION = "7.13.0";
    private static final List<String> REQUIRED_ANALYZERS = List.of("ik_max_word", "ik_smart");

    private final ElasticsearchGateway gateway;
    private final ElasticsearchSearchProperties properties;

    public SearchIndexInitializer(
            ElasticsearchGateway gateway,
            ElasticsearchSearchProperties properties
    ) {
        this.gateway = gateway;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        initialize();
    }

    public void initialize() {
        try {
            verifyRequiredAnalyzers();
            createIndexIfMissing(properties.getNoticeIndex(), SearchIndexSchema.NOTICE_DEFINITION);
            createIndexIfMissing(properties.getApplicationIndex(), SearchIndexSchema.APPLICATION_DEFINITION);
            migrateApplicationMapping();
            attachAliasIfMissing(properties.getNoticeAlias(), properties.getNoticeIndex());
            attachAliasIfMissing(properties.getApplicationAlias(), properties.getApplicationIndex());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to initialize Elasticsearch search indexes", exception);
        }
    }

    private void verifyRequiredAnalyzers() throws IOException {
        for (String analyzer : REQUIRED_ANALYZERS) {
            if (!gateway.isAnalyzerAvailable(analyzer)) {
                throw new IllegalStateException(
                        "Required Elasticsearch analyzer [" + analyzer + "] is unavailable. "
                                + "Install analysis-ik " + ELASTICSEARCH_VERSION + " before starting intelligence-service."
                );
            }
        }
    }

    private void createIndexIfMissing(String indexName, String definition) throws IOException {
        if (!gateway.indexExists(indexName)) {
            gateway.createIndex(indexName, definition);
        }
    }

    private void migrateApplicationMapping() throws IOException {
        if (!gateway.fieldMappingMatches(
                properties.getApplicationIndex(), "approverId", "long"
        )) {
            gateway.updateMapping(
                    properties.getApplicationIndex(), SearchIndexSchema.APPLICATION_APPROVER_MAPPING
            );
        }
        if (!gateway.fieldMappingMatches(
                properties.getApplicationIndex(), "approverId", "long"
        )) {
            throw new IllegalStateException("Application search index mapping is not ready");
        }
    }

    private void attachAliasIfMissing(String aliasName, String initialIndex) throws IOException {
        if (gateway.aliasTarget(aliasName) == null) {
            gateway.switchAlias(aliasName, initialIndex);
        }
    }
}
