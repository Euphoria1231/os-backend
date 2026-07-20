package com.tsy.oa.flow.controller;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.flow.dto.ApprovalRequest;
import com.tsy.oa.flow.dto.ApprovalTaskResponse;
import com.tsy.oa.flow.dto.FlowApplicationResponse;
import com.tsy.oa.flow.service.FlowService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/flow/tasks")
public class TaskController {

    private static final String EMPLOYEE_HEADER = "X-Employee-Id";

    private final FlowService flowService;

    public TaskController(FlowService flowService) {
        this.flowService = flowService;
    }

    @GetMapping("/todo")
    public ApiResponse<List<FlowApplicationResponse>> todo(
            @RequestHeader(EMPLOYEE_HEADER) Long employeeId
    ) {
        return ApiResponse.success(flowService.listTodo(employeeId));
    }

    @GetMapping("/done")
    public ApiResponse<List<ApprovalTaskResponse>> done(
            @RequestHeader(EMPLOYEE_HEADER) Long employeeId
    ) {
        return ApiResponse.success(flowService.listDone(employeeId));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<FlowApplicationResponse> approve(
            @PathVariable Long id,
            @RequestHeader(EMPLOYEE_HEADER) Long employeeId,
            @Valid @RequestBody ApprovalRequest request
    ) {
        return ApiResponse.success(flowService.approve(id, employeeId, request));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<FlowApplicationResponse> reject(
            @PathVariable Long id,
            @RequestHeader(EMPLOYEE_HEADER) Long employeeId,
            @Valid @RequestBody ApprovalRequest request
    ) {
        return ApiResponse.success(flowService.reject(id, employeeId, request));
    }
}
