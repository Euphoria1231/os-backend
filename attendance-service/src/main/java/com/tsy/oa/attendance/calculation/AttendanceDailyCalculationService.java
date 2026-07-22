package com.tsy.oa.attendance.calculation;

import com.tsy.oa.attendance.event.AttendanceAbnormalEvent;
import com.tsy.oa.attendance.event.AttendanceAbnormalEventPublisher;
import com.tsy.oa.attendance.mapper.AttendanceDailySummaryMapper;
import com.tsy.oa.attendance.mapper.AttendanceMapper;
import com.tsy.oa.attendance.model.AttendanceDailySummary;
import com.tsy.oa.attendance.model.AttendanceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AttendanceDailyCalculationService {

    private static final Logger log = LoggerFactory.getLogger(
            AttendanceDailyCalculationService.class
    );
    private static final String APPROVED = "APPROVED";
    private static final Set<String> ABNORMAL_STATUSES = Set.of(
            AttendanceStatus.LATE.name(),
            AttendanceStatus.EARLY_LEAVE.name(),
            AttendanceStatus.ABSENT.name()
    );

    private final AttendanceMapper attendanceMapper;
    private final AttendanceDailySummaryMapper summaryMapper;
    private final ApprovedLeaveProvider approvedLeaveProvider;
    private final AttendanceCalculationRule calculationRule;
    private final AttendanceAbnormalEventPublisher abnormalEventPublisher;
    private final Clock clock;

    public AttendanceDailyCalculationService(
            AttendanceMapper attendanceMapper,
            AttendanceDailySummaryMapper summaryMapper,
            ApprovedLeaveProvider approvedLeaveProvider,
            AttendanceCalculationRule calculationRule,
            AttendanceAbnormalEventPublisher abnormalEventPublisher,
            Clock clock
    ) {
        this.attendanceMapper = attendanceMapper;
        this.summaryMapper = summaryMapper;
        this.approvedLeaveProvider = approvedLeaveProvider;
        this.calculationRule = calculationRule;
        this.abnormalEventPublisher = abnormalEventPublisher;
        this.clock = clock;
    }

    @Transactional
    public AttendanceDailySummary calculate(Long employeeId, LocalDate workDate) {
        List<ApprovedLeave> approvedLeaves = approvedLeaveProvider.findApprovedLeaves(workDate);
        return calculate(employeeId, workDate, approvedLeaves);
    }

    @Transactional
    public List<AttendanceDailySummary> calculateAll(List<Long> employeeIds, LocalDate workDate) {
        List<ApprovedLeave> approvedLeaves = approvedLeaveProvider.findApprovedLeaves(workDate);
        return employeeIds.stream()
                .map(employeeId -> calculate(employeeId, workDate, approvedLeaves))
                .toList();
    }

    private AttendanceDailySummary calculate(
            Long employeeId,
            LocalDate workDate,
            List<ApprovedLeave> approvedLeaves
    ) {
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
        AttendanceDailySummary savedSummary = summaryMapper.findByEmployeeAndWorkDate(
                employeeId,
                workDate
        );
        publishAbnormalEventAfterCommit(savedSummary);
        return savedSummary;
    }

    private void publishAbnormalEventAfterCommit(AttendanceDailySummary summary) {
        if (!ABNORMAL_STATUSES.contains(summary.getStatus())) {
            return;
        }
        AttendanceAbnormalEvent event = new AttendanceAbnormalEvent(
                "attendance-abnormal:" + summary.getEmployeeId()
                        + ":" + summary.getWorkDate()
                        + ":" + summary.getStatus(),
                summary.getEmployeeId(),
                summary.getWorkDate(),
                summary.getStatus(),
                summary.getCalculatedAt(),
                UUID.randomUUID().toString()
        );
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            publishAbnormalEvent(event);
                        }
                    }
            );
            return;
        }
        publishAbnormalEvent(event);
    }

    private void publishAbnormalEvent(AttendanceAbnormalEvent event) {
        try {
            abnormalEventPublisher.publish(event);
        } catch (RuntimeException exception) {
            log.error(
                    "Failed to publish attendance abnormal event, eventId={}",
                    event.eventId(),
                    exception
            );
        }
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
