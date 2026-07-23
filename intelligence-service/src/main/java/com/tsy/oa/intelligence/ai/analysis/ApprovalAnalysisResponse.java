package com.tsy.oa.intelligence.ai.analysis;

import com.tsy.oa.intelligence.ai.model.AiCallStatus;

import java.util.List;

public record ApprovalAnalysisResponse(Long analysisId, AiCallStatus callStatus, long applicationId,
                                       String applicationSummary, List<String> riskWarnings,
                                       String suggestedDecision, String disclaimer) {
}
