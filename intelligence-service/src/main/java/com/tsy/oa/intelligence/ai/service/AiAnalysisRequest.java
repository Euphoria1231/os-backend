package com.tsy.oa.intelligence.ai.service;

import com.tsy.oa.common.error.CommonErrorCode;
import com.tsy.oa.common.exception.BusinessException;

public record AiAnalysisRequest(String requestType, String businessReferenceId, long initiatorEmployeeId, String prompt) {
    public AiAnalysisRequest {
        requireText(requestType, "requestType");
        requireText(businessReferenceId, "businessReferenceId");
        requireText(prompt, "prompt");
        if (initiatorEmployeeId <= 0) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST);
        }
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST);
        }
    }
}
