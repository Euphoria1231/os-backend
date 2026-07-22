package com.tsy.oa.intelligence.ai.service;

import com.tsy.oa.intelligence.ai.AiProvider;
import com.tsy.oa.intelligence.ai.model.AiCallResult;
import com.tsy.oa.intelligence.ai.model.AiPrompt;
import com.tsy.oa.intelligence.ai.persistence.AiAnalysisRecord;
import com.tsy.oa.intelligence.ai.persistence.AiAnalysisRecordMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AiAnalysisService {

    private static final int RESULT_SUMMARY_LIMIT = 500;

    private final AiProvider aiProvider;
    private final AiAnalysisRecordMapper recordMapper;

    public AiAnalysisService(AiProvider aiProvider, AiAnalysisRecordMapper recordMapper) {
        this.aiProvider = aiProvider;
        this.recordMapper = recordMapper;
    }

    public AiCallResult analyze(AiAnalysisRequest request) {
        long startedAt = System.nanoTime();
        AiCallResult result = aiProvider.generate(new AiPrompt(
                request.requestType(),
                request.businessReferenceId(),
                request.prompt()
        ));
        long durationMs = (System.nanoTime() - startedAt) / 1_000_000L;
        recordMapper.insert(new AiAnalysisRecord(
                request.requestType(),
                request.businessReferenceId(),
                result.status().name(),
                durationMs,
                summary(result.displayText()),
                LocalDateTime.now()
        ));
        return result;
    }

    private String summary(String displayText) {
        return displayText.length() <= RESULT_SUMMARY_LIMIT
                ? displayText
                : displayText.substring(0, RESULT_SUMMARY_LIMIT);
    }
}
