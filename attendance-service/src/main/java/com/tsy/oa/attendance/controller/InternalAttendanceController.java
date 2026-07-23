package com.tsy.oa.attendance.controller;

import com.tsy.oa.attendance.dto.MakeupEligibilityResponse;
import com.tsy.oa.attendance.dto.MakeupCompletionRequest;
import com.tsy.oa.attendance.dto.MakeupCompletionResponse;
import com.tsy.oa.attendance.service.AttendanceService;
import com.tsy.oa.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/attendance")
public class InternalAttendanceController {

    private final AttendanceService attendanceService;

    public InternalAttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @GetMapping("/records/{recordId}/makeup-eligibility")
    public ApiResponse<MakeupEligibilityResponse> makeupEligibility(
            @PathVariable Long recordId,
            @RequestParam Long employeeId
    ) {
        return ApiResponse.success(
                attendanceService.getMakeupEligibility(recordId, employeeId)
        );
    }

    @PostMapping("/records/{recordId}/makeup-completion")
    public ApiResponse<MakeupCompletionResponse> completeMakeup(
            @PathVariable Long recordId,
            @Valid @RequestBody MakeupCompletionRequest request
    ) {
        return ApiResponse.success(attendanceService.completeMakeup(recordId, request));
    }
}
