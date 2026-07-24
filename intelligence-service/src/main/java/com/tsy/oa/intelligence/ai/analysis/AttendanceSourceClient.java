package com.tsy.oa.intelligence.ai.analysis;

import com.tsy.oa.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@FeignClient(name = "attendance-service", contextId = "attendanceAnalysisSourceClient", path = "/internal/attendance")
public interface AttendanceSourceClient {
    @GetMapping("/records")
    ApiResponse<List<AttendanceSourceRecord>> records(
            @RequestParam("requesterId") long requesterId,
            @RequestParam("targetEmployeeId") long targetEmployeeId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    );
}
