package com.tsy.oa.attendance.calculation;

import java.time.LocalDate;

public record ApprovedLeave(
        Long employeeId,
        LocalDate startDate,
        LocalDate endDate,
        String status
) {
}
