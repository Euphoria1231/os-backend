package com.tsy.oa.flow.attendance;

import com.tsy.oa.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@FeignClient(name = "attendance-service", path = "/internal/attendance")
public interface AttendanceServiceClient {

    @GetMapping("/records/{recordId}/makeup-eligibility")
    ApiResponse<MakeupEligibilityResponse> getMakeupEligibility(
            @PathVariable Long recordId,
            @RequestParam Long employeeId
    );

    @PostMapping("/records/{recordId}/makeup-completion")
    ApiResponse<MakeupCompletionResponse> completeMakeup(
            @PathVariable Long recordId,
            @RequestBody MakeupCompletionRequest request
    );

    record MakeupEligibilityResponse(
            boolean eligible,
            Long attendanceRecordId,
            Long employeeId,
            LocalDate attendanceDate,
            int remainingCount
    ) {
    }

    record MakeupCompletionRequest(Long employeeId, Long applicationId) {
    }

    record MakeupCompletionResponse(
            Long applicationId,
            Long attendanceRecordId,
            Long employeeId,
            LocalDate attendanceDate,
            String attendanceStatus,
            String originalAttendanceStatus,
            int usedCount,
            int remainingCount
    ) {
    }
}
