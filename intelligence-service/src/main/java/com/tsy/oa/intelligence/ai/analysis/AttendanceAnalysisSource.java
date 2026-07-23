package com.tsy.oa.intelligence.ai.analysis;

import java.time.LocalDate;
import java.util.List;

@FunctionalInterface
public interface AttendanceAnalysisSource {
    List<AttendanceSourceRecord> findRecords(long employeeId, LocalDate startDate, LocalDate endDate);
}
