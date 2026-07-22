package com.tsy.oa.intelligence.search.index;

public final class SearchIndexSchema {

    public static final String NOTICE_DEFINITION = """
            {"settings":{"number_of_shards":1,"number_of_replicas":0},"mappings":{"dynamic":"strict","properties":{"noticeId":{"type":"long"},"title":{"type":"text","analyzer":"ik_max_word","search_analyzer":"ik_smart"},"content":{"type":"text","analyzer":"ik_max_word","search_analyzer":"ik_smart"},"publishedAt":{"type":"date"},"status":{"type":"keyword"}}}}
            """;

    public static final String APPLICATION_DEFINITION = """
            {"settings":{"number_of_shards":1,"number_of_replicas":0},"mappings":{"dynamic":"strict","properties":{"applicationId":{"type":"long"},"applicantId":{"type":"long"},"approverId":{"type":"long"},"type":{"type":"keyword"},"status":{"type":"keyword"},"reasonSummary":{"type":"text","analyzer":"ik_max_word","search_analyzer":"ik_smart"},"submittedAt":{"type":"date"},"updatedAt":{"type":"date"}}}}
            """;

    public static final String APPLICATION_APPROVER_MAPPING = """
            {"properties":{"approverId":{"type":"long"}}}
            """;

    private SearchIndexSchema() {
    }
}
