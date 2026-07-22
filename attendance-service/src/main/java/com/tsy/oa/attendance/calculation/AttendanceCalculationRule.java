package com.tsy.oa.attendance.calculation;

import com.tsy.oa.attendance.config.AttendanceClockProperties;
import com.tsy.oa.attendance.model.AttendanceDailySummary;
import com.tsy.oa.attendance.model.AttendanceRecord;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class AttendanceCalculationRule {

    private final AttendanceClockProperties clockProperties;

    public AttendanceCalculationRule(AttendanceClockProperties clockProperties) {
        this.clockProperties = clockProperties;
    }

    public AttendanceDailySummary calculate(
            Long employeeId,
            LocalDate workDate,
            AttendanceRecord record,
            boolean approvedLeave,
            int calculationVersion,
            LocalDateTime calculatedAt
    ) {
        AttendanceDailySummary summary = new AttendanceDailySummary();
        summary.setEmployeeId(employeeId);
        summary.setWorkDate(workDate);
        summary.setCalculationVersion(calculationVersion);
        summary.setCalculatedAt(calculatedAt);

        if (record != null) {
            summary.setClockInTime(record.getClockInTime());
            summary.setClockOutTime(record.getClockOutTime());
        }

        summary.setStatus(resolveStatus(record, approvedLeave).name());
        summary.setWorkHours(calculateWorkHours(record));
        return summary;
    }

    private AttendanceStatus resolveStatus(AttendanceRecord record, boolean approvedLeave) {
        if (approvedLeave) {
            return AttendanceStatus.LEAVE;
        }
        if (record == null || record.getClockInTime() == null || record.getClockOutTime() == null) {
            return AttendanceStatus.ABSENT;
        }
        if (record.getClockInTime().toLocalTime().isAfter(
                clockProperties.getWorkStartTime().plusMinutes(clockProperties.getLateThresholdMinutes())
        )) {
            return AttendanceStatus.LATE;
        }
        if (record.getClockOutTime().toLocalTime().isBefore(clockProperties.getWorkEndTime())) {
            return AttendanceStatus.EARLY_LEAVE;
        }
        return AttendanceStatus.NORMAL;
    }

    private BigDecimal calculateWorkHours(AttendanceRecord record) {
        if (record == null || record.getClockInTime() == null || record.getClockOutTime() == null
                || record.getClockOutTime().isBefore(record.getClockInTime())) {
            return BigDecimal.ZERO.setScale(2);
        }
        long minutes = Duration.between(record.getClockInTime(), record.getClockOutTime()).toMinutes();
        return BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }
}
