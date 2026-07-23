package com.tsy.oa.intelligence.ai.analysis;

import java.time.LocalDateTime;

public record AiAnalysisRecordResponse(long id, String requestType, String businessReferenceId, Long initiatorEmployeeId, String status,
                                       long durationMs, String resultSummary, LocalDateTime auditedAt) { }
