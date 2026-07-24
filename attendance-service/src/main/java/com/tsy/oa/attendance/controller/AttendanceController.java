package com.tsy.oa.attendance.controller;

import com.tsy.oa.attendance.dto.AttendanceClockConfigResponse;
import com.tsy.oa.attendance.dto.AttendanceRecordResponse;
import com.tsy.oa.attendance.dto.ClockLocationRequest;
import com.tsy.oa.attendance.dto.MakeupQuotaAssignmentRequest;
import com.tsy.oa.attendance.dto.MakeupQuotaResponse;
import com.tsy.oa.attendance.service.AttendanceService;
import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.common.log.BusinessOperationLogger;
import com.tsy.oa.common.log.HttpOperationLogContexts;
import com.tsy.oa.common.log.OperationLogContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private static final String EMPLOYEE_HEADER = "X-Employee-Id";

    private final AttendanceService attendanceService;
    private final BusinessOperationLogger operationLogger;

    public AttendanceController(
            AttendanceService attendanceService,
            BusinessOperationLogger operationLogger
    ) {
        this.attendanceService = attendanceService;
        this.operationLogger = operationLogger;
    }

    @PostMapping("/clock-in")
    public ApiResponse<AttendanceRecordResponse> clockIn(
            @RequestHeader(EMPLOYEE_HEADER) Long employeeId,
            @Valid @RequestBody ClockLocationRequest request,
            HttpServletRequest httpRequest
    ) {
        OperationLogContext context = logContext(
                httpRequest, employeeId, "CLOCK_IN", "上午打卡"
        );
        AttendanceRecordResponse response = operationLogger.execute(
                context,
                () -> attendanceService.clockIn(employeeId, request),
                result -> result.id().toString()
        );
        return ApiResponse.success(response);
    }

    @PostMapping("/clock-out")
    public ApiResponse<AttendanceRecordResponse> clockOut(
            @RequestHeader(EMPLOYEE_HEADER) Long employeeId,
            @Valid @RequestBody ClockLocationRequest request,
            HttpServletRequest httpRequest
    ) {
        OperationLogContext context = logContext(
                httpRequest, employeeId, "CLOCK_OUT", "下午打卡"
        );
        AttendanceRecordResponse response = operationLogger.execute(
                context,
                () -> attendanceService.clockOut(employeeId, request),
                result -> result.id().toString()
        );
        return ApiResponse.success(response);
    }

    @GetMapping("/clock-config")
    public ApiResponse<AttendanceClockConfigResponse> clockConfig() {
        return ApiResponse.success(attendanceService.getClockConfig());
    }

    @GetMapping("/today")
    public ApiResponse<AttendanceRecordResponse> today(
            @RequestHeader(EMPLOYEE_HEADER) Long employeeId
    ) {
        return ApiResponse.success(attendanceService.getToday(employeeId));
    }

    @GetMapping("/records")
    public ApiResponse<List<AttendanceRecordResponse>> records(
            @RequestHeader(EMPLOYEE_HEADER) Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ApiResponse.success(attendanceService.listRecords(employeeId, startDate, endDate));
    }

    @PutMapping("/makeup-quotas/{employeeId}")
    public ApiResponse<MakeupQuotaResponse> assignMakeupQuota(
            @RequestHeader(EMPLOYEE_HEADER) Long leaderId,
            @PathVariable Long employeeId,
            @Valid @RequestBody MakeupQuotaAssignmentRequest request
    ) {
        return ApiResponse.success(
                attendanceService.assignMakeupQuota(leaderId, employeeId, request)
        );
    }

    @GetMapping("/makeup-quotas/mine")
    public ApiResponse<MakeupQuotaResponse> myMakeupQuota(
            @RequestHeader(EMPLOYEE_HEADER) Long employeeId,
            @RequestParam YearMonth quotaMonth
    ) {
        return ApiResponse.success(attendanceService.getMakeupQuota(employeeId, quotaMonth));
    }

    private OperationLogContext logContext(
            HttpServletRequest request,
            Long employeeId,
            String operationType,
            String summary
    ) {
        return HttpOperationLogContexts.create(
                request,
                employeeId,
                null,
                "ATTENDANCE",
                operationType,
                "ATTENDANCE_RECORD",
                null,
                summary
        );
    }
}
