package com.tsy.oa.attendance.dto;

import java.time.LocalDate;

public record AttendanceCalculationResponse(
        LocalDate date,
        int processedCount
) {
}
