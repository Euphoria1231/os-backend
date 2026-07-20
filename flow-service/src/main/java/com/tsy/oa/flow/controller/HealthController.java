package com.tsy.oa.flow.controller;

import com.tsy.oa.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/flow")
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("flow-service");
    }
}
