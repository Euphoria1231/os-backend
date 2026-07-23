package com.tsy.oa.intelligence.search.event;

import com.tsy.oa.intelligence.search.model.ApplicationSearchDocument;
import com.tsy.oa.intelligence.search.model.NoticeSearchDocument;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Component
public class SearchDocumentNormalizer {

    private static final int MAX_REASON_SUMMARY_LENGTH = 500;
    private static final Set<String> APPLICATION_TYPES = Set.of("LEAVE", "OVERTIME", "MAKEUP");
    private static final Set<String> APPLICATION_STATUSES = Set.of("PENDING", "APPROVED", "REJECTED");

    public NoticeSearchDocument normalizeNotice(NoticeSearchDocument document) {
        if (document == null) {
            throw new IllegalStateException("notice search document is unavailable");
        }
        long noticeId = requirePositiveId(document.noticeId(), "notice");
        String title = requireText(document.title(), "notice title");
        String content = requireText(document.content(), "notice content");
        String status = requireText(document.status(), "notice status");
        if (!"PUBLISHED".equals(status)) {
            throw new IllegalStateException("notice status must be PUBLISHED");
        }
        LocalDateTime publishedAt = requireTime(document.publishedAt(), "notice publishedAt");
        return new NoticeSearchDocument(noticeId, title, content, publishedAt, status);
    }

    public ApplicationSearchDocument normalizeApplication(ApplicationSearchDocument document) {
        if (document == null) {
            throw new IllegalStateException("application search document is unavailable");
        }
        long applicationId = requirePositiveId(document.applicationId(), "application");
        long applicantId = requirePositiveId(document.applicantId(), "application applicant");
        long approverId = requirePositiveId(document.approverId(), "application approver");
        List<Long> approverIds = requireApproverIds(document.approverIds());
        String type = requireAllowedValue(document.type(), "application type", APPLICATION_TYPES);
        String status = requireAllowedValue(document.status(), "application status", APPLICATION_STATUSES);
        LocalDateTime submittedAt = requireTime(document.submittedAt(), "application submittedAt");
        LocalDateTime updatedAt = requireTime(document.updatedAt(), "application updatedAt");
        if (updatedAt.isBefore(submittedAt)) {
            throw new IllegalStateException("application updatedAt must not be before submittedAt");
        }
        long sourceVersion = requirePositiveId(document.sourceVersion(), "application source version");
        return new ApplicationSearchDocument(
                applicationId, applicantId, approverId, approverIds, type, status,
                summarize(document.reasonSummary()), submittedAt, updatedAt, sourceVersion
        );
    }

    private List<Long> requireApproverIds(List<Long> approverIds) {
        if (approverIds == null || approverIds.isEmpty()) {
            throw new IllegalStateException("application approver ids are missing");
        }
        List<Long> normalized = approverIds.stream().distinct().toList();
        if (normalized.stream().anyMatch(value -> value == null || value <= 0)) {
            throw new IllegalStateException("application approver id is missing");
        }
        return normalized;
    }

    private long requirePositiveId(long value, String fieldName) {
        if (value <= 0) {
            throw new IllegalStateException(fieldName + " id is missing");
        }
        return value;
    }

    private String requireAllowedValue(String value, String fieldName, Set<String> allowedValues) {
        String normalized = requireText(value, fieldName);
        if (!allowedValues.contains(normalized)) {
            throw new IllegalStateException(fieldName + " is unsupported");
        }
        return normalized;
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(fieldName + " is missing");
        }
        return value.trim();
    }

    private LocalDateTime requireTime(LocalDateTime value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException(fieldName + " is missing");
        }
        return value;
    }

    private String summarize(String reasonSummary) {
        if (reasonSummary == null || reasonSummary.isBlank()) {
            return "";
        }
        String normalized = reasonSummary.trim();
        if (normalized.length() <= MAX_REASON_SUMMARY_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_REASON_SUMMARY_LENGTH);
    }
}
