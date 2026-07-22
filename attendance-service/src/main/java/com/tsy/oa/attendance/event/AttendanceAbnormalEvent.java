package com.tsy.oa.attendance.event;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AttendanceAbnormalEvent(
        String eventId,
        Long employeeId,
        LocalDate workDate,
        String abnormalType,
        LocalDateTime occurredAt,
        String traceId
) {
}
