package com.tsy.oa.attendance.job;

import com.tsy.oa.attendance.AttendanceServiceApplication;
import com.tsy.oa.attendance.calculation.ApprovedLeaveProvider;
import com.tsy.oa.attendance.calculation.UserAttendanceClient;
import com.tsy.oa.attendance.mapper.AttendanceDailySummaryMapper;
import com.tsy.oa.attendance.mapper.AttendanceMapper;
import com.tsy.oa.attendance.model.AttendanceDailySummary;
import com.tsy.oa.attendance.model.AttendanceRecord;
import com.tsy.oa.common.api.ApiResponse;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = AttendanceServiceApplication.class)
class AttendanceDailyCalculationJobTests {

    private static final Long EMPLOYEE_ID = 10L;
    private static final LocalDate PREVIOUS_DATE = LocalDate.of(2026, 7, 21);

    @Autowired
    private AttendanceDailyCalculationJob job;

    @Autowired
    private AttendanceMapper attendanceMapper;

    @Autowired
    private AttendanceDailySummaryMapper summaryMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private UserAttendanceClient userAttendanceClient;

    @MockitoBean
    private ApprovedLeaveProvider approvedLeaveProvider;

    @MockitoBean
    private Clock clock;

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM attendance_daily_summary");
        jdbcTemplate.update("DELETE FROM attendance_record");
        when(userAttendanceClient.findEmployees()).thenReturn(ApiResponse.success(List.of(
                new UserAttendanceClient.EmployeeSummary(EMPLOYEE_ID, 1)
        )));
        when(approvedLeaveProvider.findApprovedLeaves(PREVIOUS_DATE)).thenReturn(List.of());
        when(clock.instant()).thenReturn(Instant.parse("2026-07-21T18:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Shanghai"));
    }

    @Test
    void registersRequiredHandlerNameAndCalculatesPreviousNaturalDate() throws Exception {
        XxlJob annotation = AttendanceDailyCalculationJob.class
                .getDeclaredMethod("execute")
                .getAnnotation(XxlJob.class);

        job.execute();

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("attendanceDailyCalculationJob");
        assertThat(summaryMapper.findByEmployeeAndWorkDate(EMPLOYEE_ID, PREVIOUS_DATE)).isNotNull();
    }

    @Test
    void keepsOneSummaryWhenHandlerIsTriggeredTwice() {
        insertCompleteAttendanceRecord();

        job.execute();
        job.execute();

        AttendanceDailySummary summary = summaryMapper.findByEmployeeAndWorkDate(EMPLOYEE_ID, PREVIOUS_DATE);
        Integer rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendance_daily_summary WHERE employee_id = ? AND work_date = ?",
                Integer.class,
                EMPLOYEE_ID,
                PREVIOUS_DATE
        );
        assertThat(rowCount).isEqualTo(1);
        assertThat(summary.getCalculationVersion()).isEqualTo(2);
    }

    private void insertCompleteAttendanceRecord() {
        AttendanceRecord record = new AttendanceRecord();
        record.setEmployeeId(EMPLOYEE_ID);
        record.setAttendanceDate(PREVIOUS_DATE);
        record.setClockInTime(LocalDateTime.of(2026, 7, 21, 9, 0));
        record.setAttendanceStatus("NORMAL");
        attendanceMapper.insert(record);
        attendanceMapper.updateClockOut(record.getId(), LocalDateTime.of(2026, 7, 21, 18, 0));
    }

}
