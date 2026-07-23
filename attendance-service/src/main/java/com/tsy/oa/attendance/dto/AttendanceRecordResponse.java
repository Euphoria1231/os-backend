package com.tsy.oa.attendance.dto;

import com.tsy.oa.attendance.model.AttendanceRecord;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AttendanceRecordResponse(
        Long id,
        Long employeeId,
        LocalDate attendanceDate,
        LocalDateTime clockInTime,
        LocalDateTime clockOutTime,
        String attendanceStatus,
        String originalAttendanceStatus,
        Long makeupApplicationId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AttendanceRecordResponse from(AttendanceRecord record) {
        return new AttendanceRecordResponse(
                record.getId(), record.getEmployeeId(), record.getAttendanceDate(), record.getClockInTime(),
                record.getClockOutTime(), record.getAttendanceStatus(), record.getOriginalAttendanceStatus(),
                record.getMakeupApplicationId(), record.getCreatedAt(), record.getUpdatedAt()
        );
    }
}
