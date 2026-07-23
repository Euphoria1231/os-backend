package com.tsy.oa.intelligence.ai.analysis;

import jakarta.validation.constraints.Size;
import com.tsy.oa.common.error.CommonErrorCode;
import com.tsy.oa.common.exception.BusinessException;

public record OfficeQuestionRequest(@Size(min = 1, max = 500) String question) {
    public OfficeQuestionRequest {
        question = question == null ? null : question.trim();
        if (question == null || question.length() < 1 || question.length() > 500) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST);
        }
    }
}
