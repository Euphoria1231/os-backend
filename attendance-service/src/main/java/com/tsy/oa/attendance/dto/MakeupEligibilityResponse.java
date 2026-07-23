package com.tsy.oa.attendance.dto;

import com.tsy.oa.attendance.model.AttendanceMakeupQuota;
import com.tsy.oa.attendance.model.AttendanceRecord;

import java.time.LocalDate;

public record MakeupEligibilityResponse(
        boolean eligible,
        Long attendanceRecordId,
        Long employeeId,
        LocalDate attendanceDate,
        int remainingCount
) {
    public static MakeupEligibilityResponse eligible(
            AttendanceRecord record,
            AttendanceMakeupQuota quota
    ) {
        return new MakeupEligibilityResponse(
                true, record.getId(), record.getEmployeeId(), record.getAttendanceDate(),
                quota.getTotalCount() - quota.getUsedCount()
        );
    }
}
