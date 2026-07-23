package com.tsy.oa.intelligence.ai.analysis;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.common.log.BusinessOperationLogger;
import com.tsy.oa.common.log.HttpOperationLogContexts;
import com.tsy.oa.common.log.OperationLogContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@Validated
@RequestMapping("/api/intelligence/ai")
public class AiAnalysisController {
    private final ApprovalAnalysisService approvalService; private final AttendanceAnalysisService attendanceService;
    private final OfficeQuestionService questionService; private final AiAnalysisRecordService recordService;
    private final BusinessOperationLogger operationLogger;
    public AiAnalysisController(ApprovalAnalysisService approvalService, AttendanceAnalysisService attendanceService,
                                OfficeQuestionService questionService, AiAnalysisRecordService recordService,
                                BusinessOperationLogger operationLogger) {
        this.approvalService = approvalService; this.attendanceService = attendanceService;
        this.questionService = questionService; this.recordService = recordService;
        this.operationLogger = operationLogger;
    }
    @PostMapping("/approvals/{applicationId}/analysis")
    public ApiResponse<ApprovalAnalysisResponse> approval(@PathVariable @Min(1) long applicationId,
            @RequestHeader("X-Employee-Id") @Min(1) long employeeId,
            @RequestHeader(value = "X-Roles", defaultValue = "") List<String> roles,
            HttpServletRequest httpRequest) {
        OperationLogContext context = logContext(
                httpRequest, employeeId, "APPROVAL_APPLICATION",
                Long.toString(applicationId), "请求智能审批分析"
        );
        return ApiResponse.success(operationLogger.execute(
                context,
                () -> approvalService.analyze(applicationId, employeeId, roles)
        )); }
    @PostMapping("/attendance/{employeeId}/analysis")
    public ApiResponse<AttendanceAnalysisResponse> attendance(@PathVariable @Min(1) long employeeId,
            @RequestHeader("X-Employee-Id") @Min(1) long requesterId,
            @RequestHeader(value = "X-Roles", defaultValue = "") List<String> roles,
            @RequestParam String month, HttpServletRequest httpRequest) {
        OperationLogContext context = logContext(
                httpRequest, requesterId, "EMPLOYEE",
                Long.toString(employeeId), "请求考勤智能分析"
        );
        return ApiResponse.success(operationLogger.execute(
                context,
                () -> attendanceService.analyze(employeeId, requesterId, roles, month)
        )); }
    @PostMapping("/office/ask")
    public ApiResponse<OfficeQuestionResponse> ask(@RequestHeader("X-Employee-Id") @Min(1) long employeeId,
            @Valid @RequestBody OfficeQuestionRequest request, HttpServletRequest httpRequest) {
        OperationLogContext context = logContext(
                httpRequest, employeeId, "AI_ANALYSIS", null, "请求智能办公问答"
        );
        return ApiResponse.success(operationLogger.execute(
                context,
                () -> questionService.ask(employeeId, request),
                result -> result.analysisId() == null ? null : result.analysisId().toString()
        )); }
    @GetMapping("/analyses/{id}")
    public ApiResponse<AiAnalysisRecordResponse> record(@PathVariable @Min(1) long id,
            @RequestHeader("X-Employee-Id") @Min(1) long employeeId,
            @RequestHeader(value = "X-Roles", defaultValue = "") List<String> roles) {
        return ApiResponse.success(recordService.get(id, employeeId, roles)); }

    private OperationLogContext logContext(
            HttpServletRequest request,
            long employeeId,
            String targetType,
            String targetId,
            String summary
    ) {
        return HttpOperationLogContexts.create(
                request,
                employeeId,
                null,
                "AI",
                "AI_ANALYSIS",
                targetType,
                targetId,
                summary
        );
    }
}
