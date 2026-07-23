package com.tsy.oa.intelligence.ai.analysis;

import com.tsy.oa.intelligence.ai.model.AiCallStatus;

public record OfficeQuestionResponse(Long analysisId, AiCallStatus callStatus, String answer, String disclaimer) {
}
