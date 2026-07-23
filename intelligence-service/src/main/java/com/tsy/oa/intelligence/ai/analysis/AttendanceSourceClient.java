package com.tsy.oa.intelligence.ai.analysis;

import com.tsy.oa.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@FeignClient(name = "attendance-service", contextId = "attendanceAnalysisSourceClient", path = "/api/attendance")
public interface AttendanceSourceClient {
    @GetMapping("/records")
    ApiResponse<List<AttendanceSourceRecord>> records(@RequestHeader("X-Employee-Id") long employeeId,
                                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate);
}
