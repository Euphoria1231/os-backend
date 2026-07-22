package com.tsy.oa.attendance.calculation;

import com.tsy.oa.attendance.mapper.AttendanceDailySummaryMapper;
import com.tsy.oa.attendance.mapper.AttendanceMapper;
import com.tsy.oa.attendance.model.AttendanceDailySummary;
import com.tsy.oa.attendance.model.AttendanceRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AttendanceDailyCalculationService {

    private static final String APPROVED = "APPROVED";

    private final AttendanceMapper attendanceMapper;
    private final AttendanceDailySummaryMapper summaryMapper;
    private final ApprovedLeaveProvider approvedLeaveProvider;
    private final AttendanceCalculationRule calculationRule;
    private final Clock clock;

    public AttendanceDailyCalculationService(
            AttendanceMapper attendanceMapper,
            AttendanceDailySummaryMapper summaryMapper,
            ApprovedLeaveProvider approvedLeaveProvider,
            AttendanceCalculationRule calculationRule,
            Clock clock
    ) {
        this.attendanceMapper = attendanceMapper;
        this.summaryMapper = summaryMapper;
        this.approvedLeaveProvider = approvedLeaveProvider;
        this.calculationRule = calculationRule;
        this.clock = clock;
    }

    @Transactional
    public AttendanceDailySummary calculate(Long employeeId, LocalDate workDate) {
        List<ApprovedLeave> approvedLeaves = approvedLeaveProvider.findApprovedLeaves(workDate);
        AttendanceRecord record = attendanceMapper.findByEmployeeAndDate(employeeId, workDate);
        AttendanceDailySummary existing = summaryMapper.findByEmployeeAndWorkDate(employeeId, workDate);
        int calculationVersion = existing == null ? 1 : existing.getCalculationVersion() + 1;

        AttendanceDailySummary summary = calculationRule.calculate(
                employeeId,
                workDate,
                record,
                hasApprovedLeave(approvedLeaves, employeeId, workDate),
                calculationVersion,
                LocalDateTime.now(clock)
        );
        summaryMapper.upsertForRecalculation(summary);
        return summaryMapper.findByEmployeeAndWorkDate(employeeId, workDate);
    }

    private boolean hasApprovedLeave(List<ApprovedLeave> leaves, Long employeeId, LocalDate workDate) {
        return leaves.stream().anyMatch(leave ->
                employeeId.equals(leave.employeeId())
                        && APPROVED.equalsIgnoreCase(leave.status())
                        && !workDate.isBefore(leave.startDate())
                        && !workDate.isAfter(leave.endDate())
        );
    }
}
