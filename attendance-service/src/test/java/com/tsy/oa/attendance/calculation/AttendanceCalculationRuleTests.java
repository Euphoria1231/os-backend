package com.tsy.oa.attendance.calculation;

import com.tsy.oa.attendance.config.AttendanceClockProperties;
import com.tsy.oa.attendance.model.AttendanceDailySummary;
import com.tsy.oa.attendance.model.AttendanceRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class AttendanceCalculationRuleTests {

    private static final Long EMPLOYEE_ID = 10L;
    private static final LocalDate WORK_DATE = LocalDate.of(2026, 7, 21);
    private static final LocalDateTime CALCULATED_AT = LocalDateTime.of(2026, 7, 21, 19, 0);

    private AttendanceCalculationRule rule;

    @BeforeEach
    void setUp() {
        AttendanceClockProperties properties = new AttendanceClockProperties();
        properties.setWorkStartTime(LocalTime.of(9, 0));
        properties.setWorkEndTime(LocalTime.of(18, 0));
        rule = new AttendanceCalculationRule(properties);
    }

    @Test
    void calculatesNormalAttendance() {
        AttendanceDailySummary summary = calculate(recordAt(9, 0, 18, 0), false);

        assertThat(summary.getStatus()).isEqualTo("NORMAL");
        assertThat(summary.getWorkHours()).isEqualByComparingTo("9.00");
    }

    @Test
    void calculatesLateAttendance() {
        AttendanceDailySummary summary = calculate(recordAt(9, 1, 18, 0), false);

        assertThat(summary.getStatus()).isEqualTo("LATE");
    }

    @Test
    void calculatesEarlyLeaveAttendance() {
        AttendanceDailySummary summary = calculate(recordAt(9, 0, 17, 59), false);

        assertThat(summary.getStatus()).isEqualTo("EARLY_LEAVE");
    }

    @Test
    void calculatesAbsentAttendanceWhenRecordIsMissing() {
        AttendanceDailySummary summary = calculate(null, false);

        assertThat(summary.getStatus()).isEqualTo("ABSENT");
        assertThat(summary.getWorkHours()).isZero();
    }

    @Test
    void calculatesLeaveBeforeAbsentWhenLeaveIsApproved() {
        AttendanceDailySummary summary = calculate(null, true);

        assertThat(summary.getStatus()).isEqualTo("LEAVE");
        assertThat(summary.getWorkHours()).isZero();
    }

    private AttendanceDailySummary calculate(AttendanceRecord record, boolean approvedLeave) {
        return rule.calculate(EMPLOYEE_ID, WORK_DATE, record, approvedLeave, 1, CALCULATED_AT);
    }

    private AttendanceRecord recordAt(int clockInHour, int clockInMinute, int clockOutHour, int clockOutMinute) {
        AttendanceRecord record = new AttendanceRecord();
        record.setEmployeeId(EMPLOYEE_ID);
        record.setAttendanceDate(WORK_DATE);
        record.setClockInTime(WORK_DATE.atTime(clockInHour, clockInMinute));
        record.setClockOutTime(WORK_DATE.atTime(clockOutHour, clockOutMinute));
        return record;
    }
}
