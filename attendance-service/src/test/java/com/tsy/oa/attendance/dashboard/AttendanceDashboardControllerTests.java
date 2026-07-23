package com.tsy.oa.attendance.dashboard;

import com.tsy.oa.attendance.AttendanceServiceApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AttendanceServiceApplication.class)
@AutoConfigureMockMvc
class AttendanceDashboardControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpAttendanceRecords() {
        jdbcTemplate.update("DELETE FROM attendance_record");
        insertRecord(1, 1, LocalDate.of(2026, 7, 1), "NORMAL");
        insertRecord(2, 2, LocalDate.of(2026, 7, 1), "LATE");
        insertRecord(3, 1, LocalDate.of(2026, 7, 2), "EARLY_LEAVE");
        insertRecord(4, 2, LocalDate.of(2026, 7, 3), "ABSENT");
        insertRecord(5, 1, LocalDate.of(2026, 8, 1), "NORMAL");
    }

    @Test
    void returnsMonthlyStatusCountsAndDailyTrend() throws Exception {
        mockMvc.perform(get("/internal/attendance/dashboard/statistics").param("month", "2026-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.month").value("2026-07"))
                .andExpect(jsonPath("$.data.normalCount").value(1))
                .andExpect(jsonPath("$.data.lateCount").value(1))
                .andExpect(jsonPath("$.data.earlyLeaveCount").value(1))
                .andExpect(jsonPath("$.data.absentCount").value(1))
                .andExpect(jsonPath("$.data.dailyTrend.length()").value(3))
                .andExpect(jsonPath("$.data.dailyTrend[0].date").value("2026-07-01"))
                .andExpect(jsonPath("$.data.dailyTrend[0].totalCount").value(2));
    }

    private void insertRecord(long id, long employeeId, LocalDate date, String status) {
        jdbcTemplate.update(
                "INSERT INTO attendance_record (id, employee_id, attendance_date, attendance_status) "
                        + "VALUES (?, ?, ?, ?)",
                id,
                employeeId,
                date,
                status
        );
    }
}
