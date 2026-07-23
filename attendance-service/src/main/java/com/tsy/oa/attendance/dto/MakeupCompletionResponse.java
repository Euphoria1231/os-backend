package com.tsy.oa.attendance.dto;

import com.tsy.oa.attendance.model.AttendanceMakeupQuota;
import com.tsy.oa.attendance.model.AttendanceRecord;

import java.time.LocalDate;

public record MakeupCompletionResponse(
        Long applicationId,
        Long attendanceRecordId,
        Long employeeId,
        LocalDate attendanceDate,
        String attendanceStatus,
        String originalAttendanceStatus,
        int usedCount,
        int remainingCount
) {
    public static MakeupCompletionResponse from(
            Long applicationId,
            AttendanceRecord record,
            AttendanceMakeupQuota quota
    ) {
        return new MakeupCompletionResponse(
                applicationId,
                record.getId(),
                record.getEmployeeId(),
                record.getAttendanceDate(),
                record.getAttendanceStatus(),
                record.getOriginalAttendanceStatus(),
                quota.getUsedCount(),
                quota.getTotalCount() - quota.getUsedCount()
        );
    }
}
