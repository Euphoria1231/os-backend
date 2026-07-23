package com.tsy.oa.user.log.controller;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.user.log.dto.OperationLogPageResponse;
import com.tsy.oa.user.log.service.OperationLogService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/user/operation-logs")
public class OperationLogController {

    private static final String EMPLOYEE_HEADER = "X-Employee-Id";
    private static final String ROLES_HEADER = "X-Roles";

    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping("/mine")
    public ApiResponse<OperationLogPageResponse> mine(
            @RequestHeader(EMPLOYEE_HEADER) Long employeeId,
            @RequestParam(required = false) String businessModule,
            @RequestParam(required = false) String operationStatus,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize
    ) {
        return ApiResponse.success(operationLogService.listMine(
                employeeId, businessModule, operationStatus,
                startTime, endTime, page, pageSize
        ));
    }

    @GetMapping
    public ApiResponse<OperationLogPageResponse> all(
            @RequestHeader(value = ROLES_HEADER, required = false) List<String> roles,
            @RequestParam(required = false) String operatorKeyword,
            @RequestParam(required = false) String businessModule,
            @RequestParam(required = false) String operationStatus,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize
    ) {
        return ApiResponse.success(operationLogService.listAll(
                roles, operatorKeyword, businessModule, operationStatus,
                startTime, endTime, page, pageSize
        ));
    }
}
