package com.tsy.oa.attendance.calculation;

import com.tsy.oa.attendance.AttendanceServiceApplication;
import com.tsy.oa.attendance.event.AttendanceAbnormalEvent;
import com.tsy.oa.attendance.event.AttendanceAbnormalEventPublisher;
import com.tsy.oa.attendance.mapper.AttendanceDailySummaryMapper;
import com.tsy.oa.attendance.mapper.AttendanceMapper;
import com.tsy.oa.attendance.model.AttendanceDailySummary;
import com.tsy.oa.attendance.model.AttendanceRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = AttendanceServiceApplication.class)
@Import(AttendanceDailyCalculationServiceTests.StubConfiguration.class)
class AttendanceDailyCalculationServiceTests {

    private static final Long EMPLOYEE_ID = 10L;
    private static final LocalDate WORK_DATE = LocalDate.of(2026, 7, 21);

    @Autowired
    private AttendanceDailyCalculationService calculationService;

    @Autowired
    private AttendanceMapper attendanceMapper;

    @Autowired
    private AttendanceDailySummaryMapper summaryMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StubApprovedLeaveProvider approvedLeaveProvider;

    @Autowired
    private InMemoryAttendanceAbnormalEventPublisher abnormalEventPublisher;

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM attendance_daily_summary");
        jdbcTemplate.update("DELETE FROM attendance_record");
        approvedLeaveProvider.reset();
        abnormalEventPublisher.clear();
    }

    @Test
    void keepsOneSummaryWhenSameEmployeeAndDateAreCalculatedTwice() {
        insertCompleteAttendanceRecord();

        calculationService.calculate(EMPLOYEE_ID, WORK_DATE);
        calculationService.calculate(EMPLOYEE_ID, WORK_DATE);

        AttendanceDailySummary summary = summaryMapper.findByEmployeeAndWorkDate(EMPLOYEE_ID, WORK_DATE);
        Integer rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendance_daily_summary WHERE employee_id = ? AND work_date = ?",
                Integer.class,
                EMPLOYEE_ID,
                WORK_DATE
        );
        assertThat(rowCount).isEqualTo(1);
        assertThat(summary.getCalculationVersion()).isEqualTo(2);
        assertThat(summary.getStatus()).isEqualTo("NORMAL");
    }

    @Test
    void writesNoSummaryWhenApprovedLeaveServiceIsUnavailable() {
        ApprovedLeaveUnavailableException failure =
                new ApprovedLeaveUnavailableException("审批服务不可用");
        approvedLeaveProvider.failWith(failure);

        assertThatThrownBy(() -> calculationService.calculate(EMPLOYEE_ID, WORK_DATE))
                .isSameAs(failure);

        Integer rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendance_daily_summary WHERE employee_id = ? AND work_date = ?",
                Integer.class,
                EMPLOYEE_ID,
                WORK_DATE
        );
        assertThat(rowCount).isZero();
    }

    @Test
    void publishesAbnormalEventWithRequiredFieldsAfterSummaryIsSaved() {
        AttendanceDailySummary summary = calculationService.calculate(EMPLOYEE_ID, WORK_DATE);

        assertThat(summary.getStatus()).isEqualTo("ABSENT");
        assertThat(abnormalEventPublisher.events()).hasSize(1);
        AttendanceAbnormalEvent event = abnormalEventPublisher.events().getFirst();
        assertThat(event.eventId()).isEqualTo(
                "attendance-abnormal:10:2026-07-21:ABSENT"
        );
        assertThat(event.employeeId()).isEqualTo(EMPLOYEE_ID);
        assertThat(event.workDate()).isEqualTo(WORK_DATE);
        assertThat(event.abnormalType()).isEqualTo("ABSENT");
        assertThat(event.occurredAt()).isEqualTo(summary.getCalculatedAt());
        assertThat(event.traceId()).isNotBlank();
    }

    @Test
    void keepsSameEventIdWhenSameAbnormalResultIsRecalculated() {
        calculationService.calculate(EMPLOYEE_ID, WORK_DATE);
        calculationService.calculate(EMPLOYEE_ID, WORK_DATE);

        assertThat(abnormalEventPublisher.events()).hasSize(2);
        assertThat(abnormalEventPublisher.events())
                .extracting(AttendanceAbnormalEvent::eventId)
                .containsOnly("attendance-abnormal:10:2026-07-21:ABSENT");
    }

    @Test
    void doesNotPublishEventForNormalOrApprovedLeaveResult() {
        insertCompleteAttendanceRecord();
        calculationService.calculate(EMPLOYEE_ID, WORK_DATE);

        jdbcTemplate.update("DELETE FROM attendance_daily_summary");
        jdbcTemplate.update("DELETE FROM attendance_record");
        approvedLeaveProvider.approveLeave(EMPLOYEE_ID, WORK_DATE);
        calculationService.calculate(EMPLOYEE_ID, WORK_DATE);

        assertThat(abnormalEventPublisher.events()).isEmpty();
    }

    private void insertCompleteAttendanceRecord() {
        AttendanceRecord record = new AttendanceRecord();
        record.setEmployeeId(EMPLOYEE_ID);
        record.setAttendanceDate(WORK_DATE);
        record.setClockInTime(LocalDateTime.of(2026, 7, 21, 9, 0));
        record.setAttendanceStatus("NORMAL");
        attendanceMapper.insert(record);
        attendanceMapper.updateClockOut(record.getId(), LocalDateTime.of(2026, 7, 21, 18, 0));
    }

    @TestConfiguration
    static class StubConfiguration {

        @Bean
        @Primary
        StubApprovedLeaveProvider stubApprovedLeaveProvider() {
            return new StubApprovedLeaveProvider();
        }

        @Bean
        @Primary
        InMemoryAttendanceAbnormalEventPublisher inMemoryAttendanceAbnormalEventPublisher() {
            return new InMemoryAttendanceAbnormalEventPublisher();
        }
    }

    static class StubApprovedLeaveProvider implements ApprovedLeaveProvider {

        private RuntimeException failure;
        private List<ApprovedLeave> approvedLeaves = List.of();

        @Override
        public List<ApprovedLeave> findApprovedLeaves(LocalDate date) {
            if (failure != null) {
                throw failure;
            }
            return approvedLeaves;
        }

        void failWith(RuntimeException failure) {
            this.failure = failure;
        }

        void reset() {
            failure = null;
            approvedLeaves = List.of();
        }

        void approveLeave(Long employeeId, LocalDate workDate) {
            approvedLeaves = List.of(new ApprovedLeave(
                    employeeId, workDate, workDate, "APPROVED"
            ));
        }
    }

    static class InMemoryAttendanceAbnormalEventPublisher
            implements AttendanceAbnormalEventPublisher {

        private final List<AttendanceAbnormalEvent> events = new ArrayList<>();

        @Override
        public void publish(AttendanceAbnormalEvent event) {
            events.add(event);
        }

        List<AttendanceAbnormalEvent> events() {
            return List.copyOf(events);
        }

        void clear() {
            events.clear();
        }
    }
}
