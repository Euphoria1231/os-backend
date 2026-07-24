package com.tsy.oa.intelligence.ai.analysis;

import java.time.LocalDate;
import java.util.List;

@FunctionalInterface
public interface AttendanceAnalysisSource {
    List<AttendanceSourceRecord> findRecords(
            long requesterId,
            long targetEmployeeId,
            LocalDate startDate,
            LocalDate endDate
    );
}
