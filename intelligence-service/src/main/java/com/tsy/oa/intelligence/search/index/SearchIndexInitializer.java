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

    private static final String NOTICE_INDEX_DEFINITION = """
            {"settings":{"number_of_shards":1,"number_of_replicas":0},"mappings":{"dynamic":"strict","properties":{"noticeId":{"type":"long"},"title":{"type":"text","analyzer":"ik_max_word","search_analyzer":"ik_smart"},"content":{"type":"text","analyzer":"ik_max_word","search_analyzer":"ik_smart"},"publishedAt":{"type":"date"},"status":{"type":"keyword"}}}}
            """;

    private static final String APPLICATION_INDEX_DEFINITION = """
            {"settings":{"number_of_shards":1,"number_of_replicas":0},"mappings":{"dynamic":"strict","properties":{"applicationId":{"type":"long"},"applicantId":{"type":"long"},"approverId":{"type":"long"},"type":{"type":"keyword"},"status":{"type":"keyword"},"reasonSummary":{"type":"text","analyzer":"ik_max_word","search_analyzer":"ik_smart"},"submittedAt":{"type":"date"},"updatedAt":{"type":"date"}}}}
            """;

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
            createIndexIfMissing(properties.getNoticeIndex(), NOTICE_INDEX_DEFINITION);
            createIndexIfMissing(properties.getApplicationIndex(), APPLICATION_INDEX_DEFINITION);
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
}
