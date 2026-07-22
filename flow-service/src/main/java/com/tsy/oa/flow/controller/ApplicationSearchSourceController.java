package com.tsy.oa.flow.controller;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.flow.dto.ApplicationSearchSourcePageResponse;
import com.tsy.oa.flow.dto.ApplicationSearchSourceResponse;
import com.tsy.oa.flow.service.FlowService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/internal/flow/search-source")
public class ApplicationSearchSourceController {

    private final FlowService flowService;

    public ApplicationSearchSourceController(FlowService flowService) {
        this.flowService = flowService;
    }

    @GetMapping("/{id}")
    public ApiResponse<ApplicationSearchSourceResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(flowService.getSearchSource(id));
    }

    @GetMapping
    public ApiResponse<ApplicationSearchSourcePageResponse> page(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "100") @Min(1) @Max(100) int pageSize
    ) {
        return ApiResponse.success(flowService.listSearchSource(page, pageSize));
    }
}
