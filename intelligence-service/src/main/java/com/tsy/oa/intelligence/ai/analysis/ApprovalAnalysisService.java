package com.tsy.oa.intelligence.ai.analysis;

import com.tsy.oa.common.error.CommonErrorCode;
import com.tsy.oa.common.exception.BusinessException;
import com.tsy.oa.intelligence.ai.model.AiCallResult;
import com.tsy.oa.intelligence.ai.model.AiCallStatus;
import com.tsy.oa.intelligence.ai.service.AiAnalysisRequest;
import com.tsy.oa.intelligence.ai.service.AiAnalysisAuditResult;
import com.tsy.oa.intelligence.ai.service.AiAnalysisService;
import com.tsy.oa.intelligence.search.event.source.ApplicationSearchSourceClient;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ApprovalAnalysisService {

    private static final DateTimeFormatter SUBMITTED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Map<String, String> APPROVAL_STATUS_LABELS = Map.of(
            "PENDING", "待审批",
            "APPROVED", "已同意",
            "REJECTED", "已驳回"
    );
    private static final List<String> ATTENDANCE_STATUS_ORDER = List.of(
            "LATE", "ABSENT", "EARLY_LEAVE", "MISSING_CLOCK_IN", "MISSING_CLOCK_OUT"
    );
    private static final Map<String, String> ATTENDANCE_STATUS_LABELS = Map.of(
            "LATE", "迟到",
            "ABSENT", "缺勤",
            "EARLY_LEAVE", "早退",
            "MISSING_CLOCK_IN", "上午缺卡",
            "MISSING_CLOCK_OUT", "下午缺卡"
    );

    private final ApplicationAnalysisSource source;
    private final AttendanceAnalysisSource attendanceSource;
    private final AiAnalysisService aiAnalysisService;
    private final AiPromptSanitizer promptSanitizer;

    public ApprovalAnalysisService(
            ApplicationAnalysisSource source,
            AttendanceAnalysisSource attendanceSource,
            AiAnalysisService aiAnalysisService,
            AiPromptSanitizer promptSanitizer
    ) {
        this.source = source;
        this.attendanceSource = attendanceSource;
        this.aiAnalysisService = aiAnalysisService;
        this.promptSanitizer = promptSanitizer;
    }

    public ApprovalAnalysisResponse analyze(long applicationId, long employeeId) {
        ApplicationSearchSourceClient.ApplicationSearchSourceResponse application = source.findById(applicationId);
        if (application == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND);
        }
        List<Long> approverIds = application.approverIds();
        if (approverIds == null || !approverIds.contains(employeeId)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        if (!"LEAVE".equalsIgnoreCase(application.applicationType())) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST);
        }

        YearMonth currentMonth = YearMonth.now();
        List<AttendanceSourceRecord> attendanceRecords = attendanceSource.findRecords(
                employeeId,
                application.applicantId(),
                currentMonth.atDay(1),
                currentMonth.atEndOfMonth()
        );
        AttendanceSummary attendance = summarizeAttendance(attendanceRecords);
        String statusLabel = approvalStatusLabel(application.status());
        String reason = promptSanitizer.sanitizeApprovalReason(application.reason()).trim();
        String summary = "请假申请；当前状态：" + statusLabel
                + "；提交时间：" + application.createdAt().format(SUBMITTED_AT_FORMATTER)
                + "；本月考勤：" + attendance.description();
        List<String> warnings = buildWarnings(reason, attendance);
        AiAnalysisAuditResult analysis = aiAnalysisService.analyzeAndRecord(new AiAnalysisRequest(
                "APPROVAL", String.valueOf(applicationId), employeeId,
                buildPrompt(application, statusLabel, currentMonth, attendance, reason)
        ));
        AiCallResult result = analysis.result();
        String advice = result.status() == AiCallStatus.SUCCESS
                ? normalizeAdvice(result.displayText())
                : "建议补充材料后再决定：AI 暂时无法完成分析，请先核对申请人的本月考勤、请假原因和工作交接安排。";
        return new ApprovalAnalysisResponse(
                analysis.analysisId(),
                result.status(),
                applicationId,
                summary,
                warnings,
                advice,
                "仅供参考，最终由审批人结合制度和实际情况决定"
        );
    }

    private AttendanceSummary summarizeAttendance(List<AttendanceSourceRecord> records) {
        List<AttendanceSourceRecord> safeRecords = records == null ? List.of() : records;
        if (safeRecords.isEmpty()) {
            return new AttendanceSummary("暂无考勤记录", 0, true);
        }

        long normalCount = countStatus(safeRecords, "NORMAL");
        List<String> details = new ArrayList<>();
        details.add("正常 " + normalCount + " 天");
        long knownCount = normalCount;
        for (String status : ATTENDANCE_STATUS_ORDER) {
            long count = countStatus(safeRecords, status);
            knownCount += count;
            if (count > 0) {
                details.add(ATTENDANCE_STATUS_LABELS.get(status) + " " + count + " 天");
            }
        }
        long otherCount = safeRecords.size() - knownCount;
        if (otherCount > 0) {
            details.add("其他异常 " + otherCount + " 天");
        }
        return new AttendanceSummary(
                "共 " + safeRecords.size() + " 天，" + String.join("、", details),
                safeRecords.size() - normalCount,
                false
        );
    }

    private long countStatus(List<AttendanceSourceRecord> records, String status) {
        return records.stream()
                .filter(record -> status.equalsIgnoreCase(record.attendanceStatus()))
                .count();
    }

    private List<String> buildWarnings(String reason, AttendanceSummary attendance) {
        List<String> warnings = new ArrayList<>();
        if (reasonIsInsufficient(reason)) {
            warnings.add("请假原因较为简略，建议先请申请人补充具体事由和必要材料。");
        }
        if (attendance.missing()) {
            warnings.add("本月暂无可用考勤记录，建议核实考勤数据是否完整。");
        } else if (attendance.abnormalCount() > 0) {
            warnings.add("申请人本月有 " + attendance.abnormalCount()
                    + " 天考勤异常，建议结合异常日期与请假事由核对是否相关。");
        } else {
            warnings.add("本月未发现明确考勤异常，可重点核实请假原因和工作交接安排。");
        }
        return List.copyOf(warnings);
    }

    private boolean reasonIsInsufficient(String reason) {
        if (reason.isBlank() || reason.length() < 6) {
            return true;
        }
        String normalized = reason.toLowerCase();
        return List.of("test", "测试", "个人原因", "有事", "原因").contains(normalized);
    }

    private String buildPrompt(
            ApplicationSearchSourceClient.ApplicationSearchSourceResponse application,
            String statusLabel,
            YearMonth currentMonth,
            AttendanceSummary attendance,
            String reason
    ) {
        return """
                你是企业 OA 系统中的请假审批辅助员，只提供分析建议，不替代审批人作出决定。
                申请类型：请假
                当前状态：%s
                提交时间：%s
                请假原因：%s
                本月考勤（%d年%d月）：%s
                请结合本月考勤和请假原因，判断这次请假是否适合批准。
                开头必须从“建议批准、建议补充材料后再决定、不建议批准”中选择一种结论，随后用 2 至 3 句自然、简洁的中文说明依据。
                信息不足时请明确需要补充什么；不要复述字段名，不要输出英文枚举、标题、项目符号或星号等格式标记，也不得编造未提供的信息。
                """.formatted(
                statusLabel,
                application.createdAt().format(SUBMITTED_AT_FORMATTER),
                reason.isBlank() ? "未填写" : reason,
                currentMonth.getYear(),
                currentMonth.getMonthValue(),
                attendance.description()
        ).trim();
    }

    private String approvalStatusLabel(String status) {
        if (status == null) {
            return "状态待核对";
        }
        return APPROVAL_STATUS_LABELS.getOrDefault(status.toUpperCase(), "状态待核对");
    }

    private String normalizeAdvice(String advice) {
        if (advice == null || advice.isBlank()) {
            return "建议补充材料后再决定：AI 未返回有效意见，请人工核对本月考勤和请假原因。";
        }
        String normalized = advice
                .replace("\r", "")
                .replaceAll("(?m)^\\s*#{1,6}\\s*", "")
                .replaceAll("(?m)^\\s*(?:[-+*]|[1-9][.)、])\\s+", "")
                .replaceAll("[*_`~]", "")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized.startsWith("仅供参考：")
                ? normalized.substring("仅供参考：".length()).trim()
                : normalized;
    }

    private record AttendanceSummary(String description, long abnormalCount, boolean missing) {
    }
}
