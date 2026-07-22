package com.tsy.oa.attendance.controller;

import com.tsy.oa.attendance.calculation.AttendanceManualCalculationService;
import com.tsy.oa.attendance.dto.AttendanceCalculationRequest;
import com.tsy.oa.attendance.dto.AttendanceCalculationResponse;
import com.tsy.oa.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/attendance/calculations")
public class AttendanceCalculationController {

    private static final String EMPLOYEE_HEADER = "X-Employee-Id";

    private final AttendanceManualCalculationService calculationService;

    public AttendanceCalculationController(AttendanceManualCalculationService calculationService) {
        this.calculationService = calculationService;
    }

    @Operation(summary = "手动核算指定日期的每日考勤")
    @PostMapping("/daily")
    public ApiResponse<AttendanceCalculationResponse> calculateDaily(
            @Parameter(hidden = true) @RequestHeader(EMPLOYEE_HEADER) Long operatorEmployeeId,
            @Parameter(description = "核算日期，格式为 YYYY-MM-DD", required = true)
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        AttendanceCalculationRequest request = new AttendanceCalculationRequest(date);
        return ApiResponse.success(calculationService.calculate(operatorEmployeeId, request));
    }
}
