package com.tsy.oa.intelligence.ai.analysis;

import com.tsy.oa.common.exception.BusinessException;
import com.tsy.oa.intelligence.ai.AiProvider;
import com.tsy.oa.intelligence.ai.model.AiCallResult;
import com.tsy.oa.intelligence.ai.model.AiCallStatus;
import com.tsy.oa.intelligence.ai.model.AiPrompt;
import com.tsy.oa.intelligence.ai.service.AiAnalysisService;
import com.tsy.oa.intelligence.ai.persistence.AiAnalysisRecord;
import com.tsy.oa.intelligence.search.event.source.ApplicationSearchSourceClient;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Task6IntelligenceAnalysisTests {

    @Test
    void approvalAnalysisAllowsApplicantAndReturnsReferenceOnlyDegradedAdvice() {
        ApprovalAnalysisService service = new ApprovalAnalysisService(
                applicationId -> new ApplicationSearchSourceClient.ApplicationSearchSourceResponse(
                        applicationId, 10L, 20L, "LEAVE", "PENDING", "家庭事务", LocalDateTime.now(), LocalDateTime.now()),
                failingAiAnalysisService()
        );

        ApprovalAnalysisResponse response = service.analyze(99L, 10L, List.of("EMPLOYEE"));

        assertThat(response.callStatus()).isEqualTo(AiCallStatus.DEGRADED);
        assertThat(response.applicationId()).isEqualTo(99L);
        assertThat(response.applicationSummary()).contains("LEAVE", "PENDING");
        assertThat(response.riskWarnings()).isNotEmpty();
        assertThat(response.suggestedDecision()).contains("仅供参考").doesNotContain("同意", "驳回");
        assertThat(response.disclaimer()).isEqualTo("仅供参考");
    }

    @Test
    void approvalAnalysisRejectsUnrelatedEmployee() {
        ApprovalAnalysisService service = new ApprovalAnalysisService(
                applicationId -> new ApplicationSearchSourceClient.ApplicationSearchSourceResponse(
                        applicationId, 10L, 20L, "LEAVE", "PENDING", "原因", LocalDateTime.now(), LocalDateTime.now()),
                failingAiAnalysisService()
        );

        assertThatThrownBy(() -> service.analyze(99L, 30L, List.of("EMPLOYEE")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void attendanceAnalysisUsesOnlyReturnedExplicitAbnormalStatusesAndValidatesMonth() {
        AttendanceAnalysisService service = new AttendanceAnalysisService(
                (employeeId, startDate, endDate) -> List.of(
                        new AttendanceSourceRecord(employeeId, LocalDate.of(2026, 7, 2), "LATE"),
                        new AttendanceSourceRecord(employeeId, LocalDate.of(2026, 7, 3), "MISSING_CLOCK_OUT")
                ),
                failingAiAnalysisService()
        );

        AttendanceAnalysisResponse response = service.analyze(10L, 10L, List.of("EMPLOYEE"), "2026-07");

        assertThat(response.riskLevel()).isEqualTo("MEDIUM");
        assertThat(response.abnormalSummary()).contains("LATE", "MISSING_CLOCK_OUT");
        assertThatThrownBy(() -> service.analyze(10L, 10L, List.of(), "2026-7"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void officeQuestionFallbackUsesFixedPolicyAndTrimsQuestion() {
        OfficeQuestionService service = new OfficeQuestionService(failingAiAnalysisService());

        OfficeQuestionResponse response = service.ask(10L, new OfficeQuestionRequest("  如何补打卡？  "));

        assertThat(response.callStatus()).isEqualTo(AiCallStatus.DEGRADED);
        assertThat(response.answer()).contains("仅供参考", "考勤");
        assertThatThrownBy(() -> new OfficeQuestionRequest(" ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void analysisRecordIsVisibleOnlyToInitiatorOrSuperAdmin() {
        AiAnalysisRecord stored = new AiAnalysisRecord("OFFICE_QA", "10", 10L, "DEGRADED", 3L,
                "仅供参考：固定制度回答已降级。", LocalDateTime.now());
        stored.setId(7L);
        AiAnalysisRecordService service = new AiAnalysisRecordService(new com.tsy.oa.intelligence.ai.persistence.AiAnalysisRecordMapper() {
            @Override public int insert(AiAnalysisRecord record) { return 1; }
            @Override public AiAnalysisRecord findById(long id) { return id == 7L ? stored : null; }
        });

        assertThat(service.get(7L, 10L, List.of("EMPLOYEE")).resultSummary()).doesNotContain("Prompt", "Key");
        assertThatThrownBy(() -> service.get(7L, 11L, List.of("EMPLOYEE"))).isInstanceOf(BusinessException.class);
        assertThat(service.get(7L, 1L, List.of("SUPER_ADMIN")).id()).isEqualTo(7L);
        assertThatThrownBy(() -> service.get(8L, 10L, List.of("EMPLOYEE"))).isInstanceOf(BusinessException.class);
    }

    private AiAnalysisService failingAiAnalysisService() {
        AiProvider provider = (AiPrompt prompt) -> new AiCallResult(AiCallStatus.DEGRADED, "AI unavailable");
        return new AiAnalysisService(provider, new com.tsy.oa.intelligence.ai.persistence.AiAnalysisRecordMapper() {
            @Override public int insert(com.tsy.oa.intelligence.ai.persistence.AiAnalysisRecord record) { record.setId(1L); return 1; }
            @Override public com.tsy.oa.intelligence.ai.persistence.AiAnalysisRecord findById(long id) { return null; }
        });
    }
}
