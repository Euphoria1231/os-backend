package com.tsy.oa.intelligence.ai.analysis;

import com.tsy.oa.common.error.CommonErrorCode;
import com.tsy.oa.common.exception.BusinessException;
import com.tsy.oa.intelligence.ai.AiProvider;
import com.tsy.oa.intelligence.ai.model.AiCallResult;
import com.tsy.oa.intelligence.ai.model.AiCallStatus;
import com.tsy.oa.intelligence.ai.model.AiPrompt;
import com.tsy.oa.intelligence.ai.persistence.AiAnalysisRecord;
import com.tsy.oa.intelligence.ai.persistence.AiAnalysisRecordMapper;
import com.tsy.oa.intelligence.ai.service.AiAnalysisService;
import com.tsy.oa.intelligence.search.event.source.ApplicationSearchSourceClient;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeaveApprovalAnalysisTests {

    @Test
    void combinesCurrentMonthAttendanceAndLeaveReasonIntoHumanReadableDecision() {
        AtomicReference<AiPrompt> capturedPrompt = new AtomicReference<>();
        long[] capturedEmployeeIds = new long[2];
        LocalDate[] capturedRange = new LocalDate[2];
        ApplicationAnalysisSource applicationSource = applicationId -> application(
                applicationId, "LEAVE", "家中老人突发不适，需要陪同就医"
        );
        AttendanceAnalysisSource attendanceSource = (requesterId, targetEmployeeId, startDate, endDate) -> {
            capturedEmployeeIds[0] = requesterId;
            capturedEmployeeIds[1] = targetEmployeeId;
            capturedRange[0] = startDate;
            capturedRange[1] = endDate;
            return List.of(
                    new AttendanceSourceRecord(targetEmployeeId, startDate, "NORMAL"),
                    new AttendanceSourceRecord(targetEmployeeId, startDate.plusDays(1), "LATE"),
                    new AttendanceSourceRecord(targetEmployeeId, startDate.plusDays(2), "ABSENT")
            );
        };
        AiProvider provider = prompt -> {
            capturedPrompt.set(prompt);
            return new AiCallResult(
                    AiCallStatus.SUCCESS,
                    "**建议批准**：请假原因具体，本月虽有两天考勤异常，但不宜仅据此否定本次申请。"
            );
        };
        ApprovalAnalysisService service = new ApprovalAnalysisService(
                applicationSource,
                attendanceSource,
                new AiAnalysisService(provider, new NoopMapper()),
                new AiPromptSanitizer()
        );

        ApprovalAnalysisResponse response = service.analyze(99L, 20L);

        YearMonth currentMonth = YearMonth.now();
        assertThat(capturedEmployeeIds).containsExactly(20L, 10L);
        assertThat(capturedRange).containsExactly(currentMonth.atDay(1), currentMonth.atEndOfMonth());
        assertThat(response.applicationSummary())
                .contains("请假申请", "待审批", "本月考勤：共 3 天，正常 1 天、迟到 1 天、缺勤 1 天")
                .doesNotContain("LEAVE", "PENDING", "NORMAL", "LATE", "ABSENT");
        assertThat(response.riskWarnings()).contains("申请人本月有 2 天考勤异常，建议结合异常日期与请假事由核对是否相关。");
        assertThat(response.suggestedDecision()).isEqualTo(
                "建议批准：请假原因具体，本月虽有两天考勤异常，但不宜仅据此否定本次申请。"
        );

        String prompt = capturedPrompt.get().content();
        assertThat(prompt)
                .contains(
                        "请假原因：家中老人突发不适，需要陪同就医",
                        "本月考勤（" + currentMonth.getYear() + "年" + currentMonth.getMonthValue() + "月）：共 3 天，正常 1 天、迟到 1 天、缺勤 1 天",
                        "建议批准、建议补充材料后再决定、不建议批准",
                        "结合本月考勤和请假原因"
                )
                .doesNotContain("LEAVE", "PENDING", "NORMAL", "LATE", "ABSENT", "**");
    }

    @Test
    void rejectsNonLeaveApplicationBeforeReadingAttendanceOrCallingAi() {
        boolean[] invoked = new boolean[2];
        ApprovalAnalysisService service = new ApprovalAnalysisService(
                applicationId -> application(applicationId, "OVERTIME", "项目上线需要加班"),
                (requesterId, targetEmployeeId, startDate, endDate) -> {
                    invoked[0] = true;
                    return List.of();
                },
                new AiAnalysisService(prompt -> {
                    invoked[1] = true;
                    return new AiCallResult(AiCallStatus.SUCCESS, "不应调用");
                }, new NoopMapper()),
                new AiPromptSanitizer()
        );

        assertThatThrownBy(() -> service.analyze(100L, 20L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(CommonErrorCode.BAD_REQUEST);
        assertThat(invoked).containsExactly(false, false);
    }

    private ApplicationSearchSourceClient.ApplicationSearchSourceResponse application(
            long applicationId,
            String applicationType,
            String reason
    ) {
        LocalDateTime now = LocalDateTime.now();
        return new ApplicationSearchSourceClient.ApplicationSearchSourceResponse(
                applicationId,
                10L,
                20L,
                List.of(20L),
                applicationType,
                "PENDING",
                reason,
                now,
                now,
                1L
        );
    }

    private static class NoopMapper implements AiAnalysisRecordMapper {
        @Override
        public int insert(AiAnalysisRecord record) {
            record.setId(1L);
            return 1;
        }

        @Override
        public AiAnalysisRecord findById(long id) {
            return null;
        }
    }
}
