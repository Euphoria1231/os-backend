package com.tsy.oa.intelligence.ai.persistence;

import java.time.LocalDateTime;

public class AiAnalysisRecord {
    private Long id;
    private final String requestType;
    private final String businessReferenceId;
    private final long initiatorEmployeeId;
    private final String status;
    private final long durationMs;
    private final String resultSummary;
    private final LocalDateTime auditedAt;

    public AiAnalysisRecord(String requestType, String businessReferenceId, long initiatorEmployeeId, String status,
                            long durationMs, String resultSummary, LocalDateTime auditedAt) {
        if (initiatorEmployeeId <= 0) {
            throw new IllegalArgumentException("initiatorEmployeeId must be positive");
        }
        this.requestType = requestType; this.businessReferenceId = businessReferenceId;
        this.initiatorEmployeeId = initiatorEmployeeId; this.status = status; this.durationMs = durationMs;
        this.resultSummary = resultSummary; this.auditedAt = auditedAt;
    }
    public AiAnalysisRecord(Long id, String requestType, String businessReferenceId, Long initiatorEmployeeId,
                            String status, Long durationMs, String resultSummary, LocalDateTime auditedAt) {
        this(requestType, businessReferenceId, initiatorEmployeeId, status, durationMs, resultSummary, auditedAt);
        this.id = id;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRequestType() { return requestType; }
    public String getBusinessReferenceId() { return businessReferenceId; }
    public long getInitiatorEmployeeId() { return initiatorEmployeeId; }
    public String getStatus() { return status; }
    public long getDurationMs() { return durationMs; }
    public String getResultSummary() { return resultSummary; }
    public LocalDateTime getAuditedAt() { return auditedAt; }
}
