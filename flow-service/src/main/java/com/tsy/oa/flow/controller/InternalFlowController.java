package com.tsy.oa.flow.controller;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.flow.dto.ApprovedLeaveResponse;
import com.tsy.oa.flow.dto.FlowSearchSourceResponse;
import com.tsy.oa.flow.service.FlowService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/internal/flow")
public class InternalFlowController {

    private final FlowService flowService;

    public InternalFlowController(FlowService flowService) {
        this.flowService = flowService;
    }

    @GetMapping("/approved-leaves")
    public ApiResponse<List<ApprovedLeaveResponse>> approvedLeaves(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.success(flowService.listApprovedLeaves(date));
    }

    @GetMapping("/search-source")
    public ApiResponse<List<FlowSearchSourceResponse>> searchSource(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(flowService.listSearchSource(page, pageSize));
    }
}
