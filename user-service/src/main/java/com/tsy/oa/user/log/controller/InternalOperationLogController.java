package com.tsy.oa.user.log.controller;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.user.log.dto.OperationLogAppendRequest;
import com.tsy.oa.user.log.dto.OperationLogResponse;
import com.tsy.oa.user.log.service.OperationLogService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/user/operation-logs")
public class InternalOperationLogController {

    private final OperationLogService operationLogService;

    public InternalOperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @PostMapping
    public ApiResponse<OperationLogResponse> append(
            @Valid @RequestBody OperationLogAppendRequest request
    ) {
        return ApiResponse.success(operationLogService.append(request));
    }
}
