package com.tsy.oa.intelligence.ai.analysis;

import com.tsy.oa.intelligence.ai.model.AiCallStatus;
import java.util.List;

public record AttendanceAnalysisResponse(Long analysisId, AiCallStatus callStatus, long employeeId, String month,
                                         String riskLevel, String abnormalSummary, List<String> improvementSuggestions,
                                         String disclaimer) {
}
