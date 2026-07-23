package com.tsy.oa.intelligence.ai.analysis;

import com.tsy.oa.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/intelligence/ai")
public class AiAnalysisController {
    private final ApprovalAnalysisService approvalService; private final AttendanceAnalysisService attendanceService;
    private final OfficeQuestionService questionService; private final AiAnalysisRecordService recordService;
    public AiAnalysisController(ApprovalAnalysisService approvalService, AttendanceAnalysisService attendanceService,
                                OfficeQuestionService questionService, AiAnalysisRecordService recordService) {
        this.approvalService = approvalService; this.attendanceService = attendanceService;
        this.questionService = questionService; this.recordService = recordService;
    }
    @PostMapping("/approvals/{applicationId}/analysis")
    public ApiResponse<ApprovalAnalysisResponse> approval(@PathVariable @Min(1) long applicationId,
            @RequestHeader("X-Employee-Id") @Min(1) long employeeId,
            @RequestHeader(value = "X-Roles", defaultValue = "") List<String> roles) {
        return ApiResponse.success(approvalService.analyze(applicationId, employeeId, roles)); }
    @PostMapping("/attendance/{employeeId}/analysis")
    public ApiResponse<AttendanceAnalysisResponse> attendance(@PathVariable @Min(1) long employeeId,
            @RequestHeader("X-Employee-Id") @Min(1) long requesterId,
            @RequestHeader(value = "X-Roles", defaultValue = "") List<String> roles, @RequestParam String month) {
        return ApiResponse.success(attendanceService.analyze(employeeId, requesterId, roles, month)); }
    @PostMapping("/office/ask")
    public ApiResponse<OfficeQuestionResponse> ask(@RequestHeader("X-Employee-Id") @Min(1) long employeeId,
                                                    @Valid @RequestBody OfficeQuestionRequest request) {
        return ApiResponse.success(questionService.ask(employeeId, request)); }
    @GetMapping("/analyses/{id}")
    public ApiResponse<AiAnalysisRecordResponse> record(@PathVariable @Min(1) long id,
            @RequestHeader("X-Employee-Id") @Min(1) long employeeId,
            @RequestHeader(value = "X-Roles", defaultValue = "") List<String> roles) {
        return ApiResponse.success(recordService.get(id, employeeId, roles)); }
}
