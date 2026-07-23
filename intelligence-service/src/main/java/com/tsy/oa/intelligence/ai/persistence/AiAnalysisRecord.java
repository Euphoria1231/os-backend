package com.tsy.oa.intelligence.ai.persistence;

import java.time.LocalDateTime;

public class AiAnalysisRecord {
    private Long id;
    private final String requestType;
    private final String businessReferenceId;
    private final Long initiatorEmployeeId;
    private final String status;
    private final long durationMs;
    private final String resultSummary;
    private final LocalDateTime auditedAt;

    public AiAnalysisRecord(String requestType, String businessReferenceId, long initiatorEmployeeId, String status,
                            long durationMs, String resultSummary, LocalDateTime auditedAt) {
        this(null, requestType, businessReferenceId, initiatorEmployeeId, status, durationMs, resultSummary, auditedAt, true);
    }

    /**
     * MyBatis hydration constructor. It deliberately accepts a null initiator only for legacy rows
     * created before requester auditing; application code must use the validating constructor above.
     */
    public AiAnalysisRecord(Long id, String requestType, String businessReferenceId, Long initiatorEmployeeId,
                            String status, Long durationMs, String resultSummary, LocalDateTime auditedAt) {
        this(id, requestType, businessReferenceId, initiatorEmployeeId, status, durationMs, resultSummary, auditedAt, false);
    }

    public static AiAnalysisRecord hydrate(Long id, String requestType, String businessReferenceId, Long initiatorEmployeeId,
                                           String status, Long durationMs, String resultSummary, LocalDateTime auditedAt) {
        return new AiAnalysisRecord(id, requestType, businessReferenceId, initiatorEmployeeId, status, durationMs,
                resultSummary, auditedAt, false);
    }

    private AiAnalysisRecord(Long id, String requestType, String businessReferenceId, Long initiatorEmployeeId,
                             String status, Long durationMs, String resultSummary, LocalDateTime auditedAt,
                             boolean validateNewRecord) {
        if (validateNewRecord && (initiatorEmployeeId == null || initiatorEmployeeId <= 0)) {
            throw new IllegalArgumentException("initiatorEmployeeId must be positive");
        }
        this.id = id; this.requestType = requestType; this.businessReferenceId = businessReferenceId;
        this.initiatorEmployeeId = initiatorEmployeeId; this.status = status; this.durationMs = durationMs;
        this.resultSummary = resultSummary; this.auditedAt = auditedAt;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRequestType() { return requestType; }
    public String getBusinessReferenceId() { return businessReferenceId; }
    public Long getInitiatorEmployeeId() { return initiatorEmployeeId; }
    public String getStatus() { return status; }
    public long getDurationMs() { return durationMs; }
    public String getResultSummary() { return resultSummary; }
    public LocalDateTime getAuditedAt() { return auditedAt; }
}
