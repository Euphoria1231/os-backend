package com.tsy.oa.intelligence.ai.analysis;

import com.tsy.oa.common.error.CommonErrorCode;
import com.tsy.oa.common.exception.BusinessException;
import com.tsy.oa.intelligence.ai.model.AiCallResult;
import com.tsy.oa.intelligence.ai.service.AiAnalysisRequest;
import com.tsy.oa.intelligence.ai.service.AiAnalysisAuditResult;
import com.tsy.oa.intelligence.ai.service.AiAnalysisService;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AttendanceAnalysisService {
    private final AttendanceAnalysisSource source;
    private final AiAnalysisService aiAnalysisService;

    public AttendanceAnalysisService(AttendanceAnalysisSource source, AiAnalysisService aiAnalysisService) {
        this.source = source;
        this.aiAnalysisService = aiAnalysisService;
    }

    public AttendanceAnalysisResponse analyze(long targetEmployeeId, long requesterId, String month) {
        if (targetEmployeeId == requesterId) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        YearMonth parsedMonth = parseMonth(month);
        List<AttendanceSourceRecord> records = source.findRecords(
                requesterId,
                targetEmployeeId,
                parsedMonth.atDay(1),
                parsedMonth.atEndOfMonth()
        );
        Map<String, Long> abnormalities = records.stream()
                .filter(record -> record.attendanceStatus() != null && !"NORMAL".equalsIgnoreCase(record.attendanceStatus()))
                .collect(Collectors.groupingBy(AttendanceSourceRecord::attendanceStatus, Collectors.counting()));
        long count = abnormalities.values().stream().mapToLong(Long::longValue).sum();
        String risk = count == 0 ? "LOW" : count == 1 ? "LOW" : count <= 3 ? "MEDIUM" : "HIGH";
        String summary = abnormalities.isEmpty() ? "未发现来源接口返回的明确异常状态。" : "明确异常：" + abnormalities;
        AiAnalysisAuditResult analysis = aiAnalysisService.analyzeAndRecord(new AiAnalysisRequest("ATTENDANCE", targetEmployeeId + "-" + month, requesterId,
                "仅基于明确考勤状态给出改进建议：" + summary));
        AiCallResult result = analysis.result();
        List<String> suggestions = result.status().name().equals("SUCCESS") ? List.of(result.displayText())
                : List.of("仅供参考：请核对迟到、缺卡等来源记录并按制度补正。" );
        return new AttendanceAnalysisResponse(analysis.analysisId(), result.status(), targetEmployeeId, month, risk, summary, suggestions, "仅供参考");
    }

    private YearMonth parseMonth(String month) {
        if (month == null || !month.matches("\\d{4}-\\d{2}")) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST);
        }
        try { return YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM")); }
        catch (DateTimeParseException exception) { throw new BusinessException(CommonErrorCode.BAD_REQUEST); }
    }
}
