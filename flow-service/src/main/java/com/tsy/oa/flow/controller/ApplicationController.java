package com.tsy.oa.flow.controller;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.common.log.BusinessOperationLogger;
import com.tsy.oa.common.log.HttpOperationLogContexts;
import com.tsy.oa.common.log.OperationLogContext;
import com.tsy.oa.flow.dto.ApplicationRequest;
import com.tsy.oa.flow.dto.FlowApplicationResponse;
import com.tsy.oa.flow.dto.MakeupApplicationRequest;
import com.tsy.oa.flow.service.FlowService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/flow/applications")
public class ApplicationController {

    private static final String EMPLOYEE_HEADER = "X-Employee-Id";
    private static final String ROLES_HEADER = "X-Roles";
    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    private final FlowService flowService;
    private final BusinessOperationLogger operationLogger;

    public ApplicationController(
            FlowService flowService,
            BusinessOperationLogger operationLogger
    ) {
        this.flowService = flowService;
        this.operationLogger = operationLogger;
    }

    @PostMapping("/leave")
    public ApiResponse<FlowApplicationResponse> submitLeave(
            @RequestHeader(EMPLOYEE_HEADER) Long employeeId,
            @Valid @RequestBody ApplicationRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(submit(
                httpRequest,
                employeeId,
                "SUBMIT_LEAVE",
                "提交请假申请",
                () -> flowService.submitLeave(employeeId, request)
        ));
    }

    @PostMapping("/overtime")
    public ApiResponse<FlowApplicationResponse> submitOvertime(
            @RequestHeader(EMPLOYEE_HEADER) Long employeeId,
            @Valid @RequestBody ApplicationRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(submit(
                httpRequest,
                employeeId,
                "SUBMIT_OVERTIME",
                "提交加班申请",
                () -> flowService.submitOvertime(employeeId, request)
        ));
    }

    @PostMapping("/makeup")
    public ApiResponse<FlowApplicationResponse> submitMakeup(
            @RequestHeader(EMPLOYEE_HEADER) Long employeeId,
            @Valid @RequestBody MakeupApplicationRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(submit(
                httpRequest,
                employeeId,
                "SUBMIT_MAKEUP",
                "提交补签申请，考勤记录ID：" + request.attendanceRecordId(),
                () -> flowService.submitMakeup(employeeId, request)
        ));
    }

    @GetMapping("/mine")
    public ApiResponse<List<FlowApplicationResponse>> mine(
            @RequestHeader(EMPLOYEE_HEADER) Long employeeId
    ) {
        return ApiResponse.success(flowService.listMyApplications(employeeId));
    }

    @GetMapping("/{applicationId}")
    public ApiResponse<FlowApplicationResponse> detail(
            @PathVariable Long applicationId,
            @RequestHeader(EMPLOYEE_HEADER) Long employeeId,
            @RequestHeader(value = ROLES_HEADER, defaultValue = "") List<String> roles
    ) {
        return ApiResponse.success(flowService.getApplicationDetail(
                applicationId,
                employeeId,
                roles.contains(SUPER_ADMIN_ROLE)
        ));
    }

    private FlowApplicationResponse submit(
            HttpServletRequest request,
            Long employeeId,
            String operationType,
            String summary,
            java.util.function.Supplier<FlowApplicationResponse> action
    ) {
        OperationLogContext context = HttpOperationLogContexts.create(
                request,
                employeeId,
                null,
                "FLOW",
                operationType,
                "APPLICATION",
                null,
                summary
        );
        return operationLogger.execute(context, action, result -> result.id().toString());
    }
}
