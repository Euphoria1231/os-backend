package com.tsy.oa.attendance.mapper;

import com.tsy.oa.attendance.model.AttendanceDailySummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = com.tsy.oa.attendance.AttendanceServiceApplication.class)
class AttendanceDailySummaryMapperTests {

    private static final Long EMPLOYEE_ID = 10L;
    private static final LocalDate WORK_DATE = LocalDate.of(2026, 7, 21);

    @Autowired
    private AttendanceDailySummaryMapper mapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearDailySummaries() {
        jdbcTemplate.update("DELETE FROM attendance_daily_summary");
    }

    @Test
    void rejectsDuplicateInsertForSameEmployeeAndWorkDate() {
        mapper.insert(summary("NORMAL", "8.00", 1));

        assertThatThrownBy(() -> mapper.insert(summary("LATE", "7.50", 2)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void updatesExistingSummaryWhenRecalculated() {
        mapper.insert(summary("LATE", "7.50", 1));

        AttendanceDailySummary recalculated = summary("NORMAL", "8.00", 2);
        recalculated.setClockInTime(LocalDateTime.of(2026, 7, 21, 9, 0));
        mapper.upsertForRecalculation(recalculated);

        AttendanceDailySummary actual = mapper.findByEmployeeAndWorkDate(EMPLOYEE_ID, WORK_DATE);
        Integer rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendance_daily_summary WHERE employee_id = ? AND work_date = ?",
                Integer.class,
                EMPLOYEE_ID,
                WORK_DATE
        );

        assertThat(rowCount).isEqualTo(1);
        assertThat(actual.getStatus()).isEqualTo("NORMAL");
        assertThat(actual.getWorkHours()).isEqualByComparingTo("8.00");
        assertThat(actual.getCalculationVersion()).isEqualTo(2);
        assertThat(actual.getClockInTime()).isEqualTo(LocalDateTime.of(2026, 7, 21, 9, 0));
    }

    private AttendanceDailySummary summary(String status, String workHours, int calculationVersion) {
        AttendanceDailySummary summary = new AttendanceDailySummary();
        summary.setEmployeeId(EMPLOYEE_ID);
        summary.setWorkDate(WORK_DATE);
        summary.setClockInTime(LocalDateTime.of(2026, 7, 21, 9, 5));
        summary.setClockOutTime(LocalDateTime.of(2026, 7, 21, 17, 0));
        summary.setWorkHours(new BigDecimal(workHours));
        summary.setStatus(status);
        summary.setCalculationVersion(calculationVersion);
        summary.setCalculatedAt(LocalDateTime.of(2026, 7, 21, 18, 0));
        return summary;
    }
}
