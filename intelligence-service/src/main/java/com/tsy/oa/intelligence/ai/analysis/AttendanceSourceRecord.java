package com.tsy.oa.intelligence.ai.analysis;

import java.time.LocalDate;

public record AttendanceSourceRecord(long employeeId, LocalDate attendanceDate, String attendanceStatus) {
}
