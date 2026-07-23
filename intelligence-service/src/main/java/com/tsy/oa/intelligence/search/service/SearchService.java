package com.tsy.oa.intelligence.search.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tsy.oa.common.error.CommonErrorCode;
import com.tsy.oa.common.exception.BusinessException;
import com.tsy.oa.intelligence.search.config.ElasticsearchSearchProperties;
import com.tsy.oa.intelligence.search.dto.ApplicationSearchResponse;
import com.tsy.oa.intelligence.search.dto.NoticeSearchResponse;
import com.tsy.oa.intelligence.search.dto.SearchPageResponse;
import com.tsy.oa.intelligence.search.model.ApplicationSearchDocument;
import com.tsy.oa.intelligence.search.model.NoticeSearchDocument;
import com.tsy.oa.intelligence.search.repository.ElasticsearchGateway;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class SearchService {

    private static final Set<String> APPLICATION_TYPES = Set.of("LEAVE", "OVERTIME", "MAKEUP");
    private static final Set<String> APPLICATION_STATUSES = Set.of("PENDING", "APPROVED", "REJECTED");
    private static final int ELASTICSEARCH_RESULT_WINDOW = 10_000;

    private final ElasticsearchGateway gateway;
    private final ObjectMapper objectMapper;
    private final ElasticsearchSearchProperties properties;

    public SearchService(
            ElasticsearchGateway gateway,
            ObjectMapper objectMapper,
            ElasticsearchSearchProperties properties
    ) {
        this.gateway = gateway;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public SearchPageResponse<NoticeSearchResponse> searchNotices(
            String keyword,
            int page,
            int pageSize
    ) {
        ObjectNode request = baseRequest(page, pageSize);
        ObjectNode bool = request.putObject("query").putObject("bool");
        addTextQuery(bool, keyword, List.of("title^2", "content"));
        bool.putArray("filter").add(termQuery("status", "PUBLISHED"));
        request.putArray("sort")
                .add(sortField("publishedAt", "desc"))
                .add(sortField("noticeId", "desc"));
        ObjectNode highlight = request.putObject("highlight");
        highlight.putArray("pre_tags").add("<em>");
        highlight.putArray("post_tags").add("</em>");
        highlight.putObject("fields").putObject("title");
        highlight.withObject("fields").putObject("content");

        JsonNode hits = execute(properties.getNoticeAlias(), request).path("hits");
        List<NoticeSearchResponse> items = new ArrayList<>();
        for (JsonNode hit : hits.path("hits")) {
            NoticeSearchDocument source = treeToValue(hit.path("_source"), NoticeSearchDocument.class);
            items.add(new NoticeSearchResponse(
                    source.noticeId(), source.title(), source.content(),
                    firstHighlight(hit, "title"), firstHighlight(hit, "content"), source.publishedAt()
            ));
        }
        return new SearchPageResponse<>(items, hits.path("total").path("value").asLong(), page, pageSize);
    }

    public SearchPageResponse<ApplicationSearchResponse> searchApplications(
            String keyword,
            String type,
            String status,
            int page,
            int pageSize,
            long employeeId,
            boolean administrator
    ) {
        String normalizedType = normalizeFilter(type, APPLICATION_TYPES);
        String normalizedStatus = normalizeFilter(status, APPLICATION_STATUSES);
        ObjectNode request = baseRequest(page, pageSize);
        ObjectNode bool = request.putObject("query").putObject("bool");
        addTextQuery(bool, keyword, List.of("reasonSummary"));
        ArrayNode filters = bool.putArray("filter");
        if (!administrator) {
            ObjectNode ownerScope = objectMapper.createObjectNode();
            ObjectNode ownerScopeBool = ownerScope.putObject("bool");
            ownerScopeBool.putArray("should")
                    .add(termQuery("applicantId", employeeId))
                    .add(termQuery("approverIds", employeeId));
            ownerScopeBool.put("minimum_should_match", 1);
            filters.add(ownerScope);
        }
        if (normalizedType != null) {
            filters.add(termQuery("type", normalizedType));
        }
        if (normalizedStatus != null) {
            filters.add(termQuery("status", normalizedStatus));
        }
        request.putArray("sort")
                .add(sortField("submittedAt", "desc"))
                .add(sortField("applicationId", "desc"));
        ObjectNode highlight = request.putObject("highlight");
        highlight.putArray("pre_tags").add("<em>");
        highlight.putArray("post_tags").add("</em>");
        highlight.putObject("fields").putObject("reasonSummary");

        JsonNode hits = execute(properties.getApplicationAlias(), request).path("hits");
        List<ApplicationSearchResponse> items = new ArrayList<>();
        for (JsonNode hit : hits.path("hits")) {
            ApplicationSearchDocument source = treeToValue(
                    hit.path("_source"), ApplicationSearchDocument.class
            );
            items.add(new ApplicationSearchResponse(
                    source.applicationId(), source.applicantId(), source.type(), source.status(),
                    source.reasonSummary(), firstHighlight(hit, "reasonSummary"),
                    source.submittedAt(), source.updatedAt()
            ));
        }
        return new SearchPageResponse<>(items, hits.path("total").path("value").asLong(), page, pageSize);
    }

    private ObjectNode baseRequest(int page, int pageSize) {
        long from = (long) (page - 1) * pageSize;
        if (from + pageSize > ELASTICSEARCH_RESULT_WINDOW) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST);
        }
        ObjectNode request = objectMapper.createObjectNode();
        request.put("from", from);
        request.put("size", pageSize);
        request.put("track_total_hits", true);
        return request;
    }

    private void addTextQuery(ObjectNode bool, String keyword, List<String> fields) {
        ArrayNode must = bool.putArray("must");
        if (keyword == null || keyword.isBlank()) {
            must.add(objectMapper.createObjectNode().putObject("match_all"));
            return;
        }
        ObjectNode multiMatch = must.addObject().putObject("multi_match");
        multiMatch.put("query", keyword.trim());
        multiMatch.put("type", "best_fields");
        ArrayNode fieldNames = multiMatch.putArray("fields");
        fields.forEach(fieldNames::add);
    }

    private ObjectNode termQuery(String field, String value) {
        ObjectNode query = objectMapper.createObjectNode();
        query.putObject("term").put(field, value);
        return query;
    }

    private ObjectNode termQuery(String field, long value) {
        ObjectNode query = objectMapper.createObjectNode();
        query.putObject("term").put(field, value);
        return query;
    }

    private ObjectNode sortField(String field, String direction) {
        ObjectNode sort = objectMapper.createObjectNode();
        sort.putObject(field).put("order", direction);
        return sort;
    }

    private String normalizeFilter(String value, Set<String> allowed) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if (!allowed.contains(normalized)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST);
        }
        return normalized;
    }

    private JsonNode execute(String indexName, JsonNode request) {
        try {
            return objectMapper.readTree(gateway.search(indexName, objectMapper.writeValueAsString(request)));
        } catch (IOException exception) {
            throw new IllegalStateException("Elasticsearch search request failed", exception);
        }
    }

    private <T> T treeToValue(JsonNode source, Class<T> type) {
        try {
            return objectMapper.treeToValue(source, type);
        } catch (IOException exception) {
            throw new IllegalStateException("Elasticsearch search response is invalid", exception);
        }
    }

    private String firstHighlight(JsonNode hit, String field) {
        JsonNode values = hit.path("highlight").path(field);
        return values.isArray() && !values.isEmpty() ? values.get(0).asText() : null;
    }
}
