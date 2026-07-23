package com.tsy.oa.flow.controller;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.flow.dto.ApplicationRequest;
import com.tsy.oa.flow.dto.FlowApplicationResponse;
import com.tsy.oa.flow.dto.MakeupApplicationRequest;
import com.tsy.oa.flow.service.FlowService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
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

    private final FlowService flowService;

    public ApplicationController(FlowService flowService) {
        this.flowService = flowService;
    }

    @PostMapping("/leave")
    public ApiResponse<FlowApplicationResponse> submitLeave(
            @RequestHeader(EMPLOYEE_HEADER) Long employeeId,
            @Valid @RequestBody ApplicationRequest request
    ) {
        return ApiResponse.success(flowService.submitLeave(employeeId, request));
    }

    @PostMapping("/overtime")
    public ApiResponse<FlowApplicationResponse> submitOvertime(
            @RequestHeader(EMPLOYEE_HEADER) Long employeeId,
            @Valid @RequestBody ApplicationRequest request
    ) {
        return ApiResponse.success(flowService.submitOvertime(employeeId, request));
    }

    @PostMapping("/makeup")
    public ApiResponse<FlowApplicationResponse> submitMakeup(
            @RequestHeader(EMPLOYEE_HEADER) Long employeeId,
            @Valid @RequestBody MakeupApplicationRequest request
    ) {
        return ApiResponse.success(flowService.submitMakeup(employeeId, request));
    }

    @GetMapping("/mine")
    public ApiResponse<List<FlowApplicationResponse>> mine(
            @RequestHeader(EMPLOYEE_HEADER) Long employeeId
    ) {
        return ApiResponse.success(flowService.listMyApplications(employeeId));
    }
}
