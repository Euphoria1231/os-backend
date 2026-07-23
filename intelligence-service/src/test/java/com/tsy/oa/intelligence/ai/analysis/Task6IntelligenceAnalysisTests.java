package com.tsy.oa.intelligence.ai.analysis;

import com.tsy.oa.common.exception.BusinessException;
import com.tsy.oa.common.error.CommonErrorCode;
import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.intelligence.ai.AiProvider;
import com.tsy.oa.intelligence.ai.model.AiCallResult;
import com.tsy.oa.intelligence.ai.model.AiCallStatus;
import com.tsy.oa.intelligence.ai.model.AiPrompt;
import com.tsy.oa.intelligence.ai.service.AiAnalysisService;
import com.tsy.oa.intelligence.ai.service.AiAnalysisRequest;
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
                failingAiAnalysisService(), new AiPromptSanitizer()
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
                failingAiAnalysisService(), new AiPromptSanitizer()
        );

        assertThatThrownBy(() -> service.analyze(99L, 30L, List.of("EMPLOYEE")))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(CommonErrorCode.FORBIDDEN);
    }

    @Test
    void approvalAnalysisAllowsCurrentApproverAndSuperAdminAndPreservesProviderStatuses() {
        ApprovalAnalysisService service = new ApprovalAnalysisService(
                applicationId -> new ApplicationSearchSourceClient.ApplicationSearchSourceResponse(
                        applicationId, 10L, 20L, "LEAVE", "PENDING", "原因", LocalDateTime.now(), LocalDateTime.now()),
                analysisService(AiCallStatus.SUCCESS, "可作为补充意见"), new AiPromptSanitizer()
        );
        assertThat(service.analyze(99L, 20L, List.of("EMPLOYEE")).callStatus()).isEqualTo(AiCallStatus.SUCCESS);
        assertThat(service.analyze(99L, 1L, List.of("SUPER_ADMIN")).callStatus()).isEqualTo(AiCallStatus.SUCCESS);
        ApprovalAnalysisService failed = new ApprovalAnalysisService(
                applicationId -> new ApplicationSearchSourceClient.ApplicationSearchSourceResponse(
                        applicationId, 10L, 20L, "LEAVE", "PENDING", "原因", LocalDateTime.now(), LocalDateTime.now()),
                analysisService(AiCallStatus.FAILED, "upstream failure"), new AiPromptSanitizer()
        );
        assertThat(failed.analyze(99L, 10L, List.of("EMPLOYEE")).callStatus()).isEqualTo(AiCallStatus.FAILED);
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
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(CommonErrorCode.BAD_REQUEST);
    }

    @Test
    void attendanceAnalysisUsesFirstAndLastNaturalDayAndRejectsOtherEmployee() {
        final LocalDate[] range = new LocalDate[2];
        AttendanceAnalysisService service = new AttendanceAnalysisService(
                (employeeId, startDate, endDate) -> { range[0] = startDate; range[1] = endDate; return List.of(); },
                failingAiAnalysisService()
        );
        service.analyze(10L, 10L, List.of("EMPLOYEE"), "2026-02");
        assertThat(range[0]).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(range[1]).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThatThrownBy(() -> service.analyze(10L, 11L, List.of("DEPARTMENT_MANAGER"), "2026-02"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(CommonErrorCode.FORBIDDEN);
    }

    @Test
    void officeQuestionFallbackUsesFixedPolicyAndTrimsQuestion() {
        OfficeQuestionService service = new OfficeQuestionService(failingAiAnalysisService());

        OfficeQuestionResponse response = service.ask(10L, new OfficeQuestionRequest("  如何补打卡？  "));

        assertThat(response.callStatus()).isEqualTo(AiCallStatus.DEGRADED);
        assertThat(response.answer()).contains("仅供参考", "考勤");
        assertThatThrownBy(() -> new OfficeQuestionRequest(" "))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(CommonErrorCode.BAD_REQUEST);
    }

    @Test
    void officeQuestionAcceptsTrimmedFiveHundredCharactersAndRejectsFiveHundredOne() {
        OfficeQuestionRequest accepted = new OfficeQuestionRequest(" " + "a".repeat(500) + " ");
        assertThat(accepted.question()).hasSize(500);
        assertThatThrownBy(() -> new OfficeQuestionRequest("a".repeat(501)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(CommonErrorCode.BAD_REQUEST);
    }

    @Test
    void feignApprovalSourceDistinguishesNotFoundFromUpstreamFailure() {
        ApplicationSearchSourceClient notFoundClient = applicationId -> ApiResponse.failure(CommonErrorCode.NOT_FOUND);
        ApplicationSearchSourceClient failedClient = applicationId -> ApiResponse.failure(CommonErrorCode.INTERNAL_SERVER_ERROR);

        assertThat(new FeignApplicationAnalysisSource(notFoundClient).findById(99L)).isNull();
        assertThatThrownBy(() -> new FeignApplicationAnalysisSource(failedClient).findById(99L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void analysisRequestRejectsUnattributableInitiator() {
        assertThatThrownBy(() -> new AiAnalysisRequest("APPROVAL", "99", 0L, "safe prompt"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(CommonErrorCode.BAD_REQUEST);
        assertThatThrownBy(() -> new AiAnalysisRecord("APPROVAL", "99", 0L, "SUCCESS", 1L,
                "仅供参考：controlled summary", LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class);
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
        assertThatThrownBy(() -> service.get(7L, 11L, List.of("EMPLOYEE")))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(CommonErrorCode.FORBIDDEN);
        assertThat(service.get(7L, 1L, List.of("SUPER_ADMIN")).id()).isEqualTo(7L);
        assertThatThrownBy(() -> service.get(8L, 10L, List.of("EMPLOYEE")))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(CommonErrorCode.NOT_FOUND);
    }

    @Test
    void legacyUnattributedRecordIsSuperAdminOnly() {
        AiAnalysisRecord legacy = AiAnalysisRecord.hydrate(8L, "APPROVAL", "100", null, "SUCCESS", 2L,
                "仅供参考：历史受控摘要", LocalDateTime.now());
        AiAnalysisRecordService service = new AiAnalysisRecordService(new com.tsy.oa.intelligence.ai.persistence.AiAnalysisRecordMapper() {
            @Override public int insert(AiAnalysisRecord record) { return 1; }
            @Override public AiAnalysisRecord findById(long id) { return id == 8L ? legacy : null; }
        });

        assertThatThrownBy(() -> service.get(8L, 10L, List.of("EMPLOYEE")))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(CommonErrorCode.FORBIDDEN);
        assertThat(service.get(8L, 1L, List.of("SUPER_ADMIN")).initiatorEmployeeId()).isNull();
    }

    @Test
    void feignAttendanceSourceRejectsMissingOrInvalidUpstreamPayloadButAllowsEmptyMonth() {
        AttendanceSourceClient nullResponse = (employeeId, startDate, endDate) -> null;
        AttendanceSourceClient failedResponse = (employeeId, startDate, endDate) -> ApiResponse.failure(CommonErrorCode.INTERNAL_SERVER_ERROR);
        AttendanceSourceClient nullData = (employeeId, startDate, endDate) -> new ApiResponse<>(0, "success", null);
        AttendanceSourceClient emptyMonth = (employeeId, startDate, endDate) -> ApiResponse.success(List.of());

        assertThatThrownBy(() -> new FeignAttendanceAnalysisSource(nullResponse).findRecords(1L, LocalDate.now(), LocalDate.now()))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new FeignAttendanceAnalysisSource(failedResponse).findRecords(1L, LocalDate.now(), LocalDate.now()))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new FeignAttendanceAnalysisSource(nullData).findRecords(1L, LocalDate.now(), LocalDate.now()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(new FeignAttendanceAnalysisSource(emptyMonth).findRecords(1L, LocalDate.now(), LocalDate.now())).isEmpty();
    }

    private AiAnalysisService failingAiAnalysisService() {
        return analysisService(AiCallStatus.DEGRADED, "AI unavailable");
    }

    private AiAnalysisService analysisService(AiCallStatus status, String displayText) {
        AiProvider provider = (AiPrompt prompt) -> new AiCallResult(status, displayText);
        return new AiAnalysisService(provider, new com.tsy.oa.intelligence.ai.persistence.AiAnalysisRecordMapper() {
            @Override public int insert(com.tsy.oa.intelligence.ai.persistence.AiAnalysisRecord record) { record.setId(1L); return 1; }
            @Override public com.tsy.oa.intelligence.ai.persistence.AiAnalysisRecord findById(long id) { return null; }
        });
    }
}
