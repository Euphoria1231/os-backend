package com.tsy.oa.flow.attendance;

import java.time.LocalDate;

public interface AttendanceMakeupGateway {

    MakeupEligibility getEligibility(Long attendanceRecordId, Long employeeId);

    void completeMakeup(Long attendanceRecordId, Long employeeId, Long applicationId);

    record MakeupEligibility(
            Long attendanceRecordId,
            Long employeeId,
            LocalDate attendanceDate,
            int remainingCount
    ) {
    }
}
