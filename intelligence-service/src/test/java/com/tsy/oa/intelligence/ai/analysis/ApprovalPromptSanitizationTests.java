package com.tsy.oa.intelligence.ai.analysis;

import com.tsy.oa.intelligence.ai.AiProvider;
import com.tsy.oa.intelligence.ai.model.AiCallResult;
import com.tsy.oa.intelligence.ai.model.AiCallStatus;
import com.tsy.oa.intelligence.ai.model.AiPrompt;
import com.tsy.oa.intelligence.ai.service.AiAnalysisService;
import com.tsy.oa.intelligence.search.event.source.ApplicationSearchSourceClient;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ApprovalPromptSanitizationTests {
    @Test
    void removesSecretsAndPersonalDataBeforeSubmittingApprovalReasonToAi() {
        AtomicReference<AiPrompt> captured = new AtomicReference<>();
        AiProvider provider = prompt -> { captured.set(prompt); return new AiCallResult(AiCallStatus.SUCCESS, "safe"); };
        AiAnalysisService aiService = new AiAnalysisService(provider, new NoopMapper());
        String reason = "Bearer ab~+/=cd DASHSCOPE_API_KEY=sk-live-value JWT_SECRET=hunter2 access_token=abcd "
                + "{\"token\":\"json-secret\"} \"password\":\"correct horse battery staple\" api_key='sk live value' "
                + "password=letmein apiKey=key123 secret=hide "
                + "phone 13800138000 138-0013-8000 138 0013 8000 email user@example.com id 11010519491231002X "
                + "x".repeat(2500);
        ApprovalAnalysisService service = new ApprovalAnalysisService(id ->
                new ApplicationSearchSourceClient.ApplicationSearchSourceResponse(id, 10L, 20L, "LEAVE", "PENDING", reason,
                        LocalDateTime.now(), LocalDateTime.now()), aiService, new AiPromptSanitizer());

        service.analyze(99L, 20L);

        String prompt = captured.get().content();
        assertThat(prompt).doesNotContain("ab~+/=cd", "sk-live-value", "hunter2", "abcd", "json-secret",
                "letmein", "key123", "hide", "13800138000", "138-0013-8000", "138 0013 8000",
                "user@example.com", "11010519491231002X", "correct horse battery staple", "horse", "battery",
                "staple", "sk live value", "live", "value");
        assertThat(prompt).hasSizeLessThan(1300);
    }

    @Test
    void preservesOrdinaryChineseApprovalReasonWithoutCredentialAssignment() {
        AiPromptSanitizer sanitizer = new AiPromptSanitizer();

        String sanitized = sanitizer.sanitizeApprovalReason("因家中突发情况，申请请假两天处理事务。");

        assertThat(sanitized).isEqualTo("因家中突发情况，申请请假两天处理事务。");
    }

    private static class NoopMapper implements com.tsy.oa.intelligence.ai.persistence.AiAnalysisRecordMapper {
        @Override public int insert(com.tsy.oa.intelligence.ai.persistence.AiAnalysisRecord record) { record.setId(1L); return 1; }
        @Override public com.tsy.oa.intelligence.ai.persistence.AiAnalysisRecord findById(long id) { return null; }
    }
}
