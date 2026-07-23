package com.tsy.oa.intelligence.ai.analysis;

import java.time.LocalDateTime;

public record AiAnalysisRecordResponse(long id, String requestType, String businessReferenceId, String status,
                                       long durationMs, String resultSummary, LocalDateTime auditedAt) { }
