package com.tsy.oa.flow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = FlowServiceApplication.class)
@AutoConfigureMockMvc
class FlowDashboardControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpApplications() {
        jdbcTemplate.update("DELETE FROM approval_record");
        jdbcTemplate.update("DELETE FROM flow_application");
        insertApplication(1, "LEAVE", "PENDING", LocalDateTime.of(2026, 7, 1, 9, 0));
        insertApplication(2, "LEAVE", "APPROVED", LocalDateTime.of(2026, 7, 1, 10, 0));
        insertApplication(3, "OVERTIME", "APPROVED", LocalDateTime.of(2026, 7, 2, 9, 0));
        insertApplication(4, "OVERTIME", "REJECTED", LocalDateTime.of(2026, 7, 3, 9, 0));
        insertApplication(5, "LEAVE", "PENDING", LocalDateTime.of(2026, 8, 1, 9, 0));
    }

    @Test
    void returnsMonthlyStatusTypeAndDailyStatistics() throws Exception {
        mockMvc.perform(get("/internal/flow/dashboard/statistics").param("month", "2026-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.month").value("2026-07"))
                .andExpect(jsonPath("$.data.pendingCount").value(1))
                .andExpect(jsonPath("$.data.approvedCount").value(2))
                .andExpect(jsonPath("$.data.rejectedCount").value(1))
                .andExpect(jsonPath("$.data.typeDistribution[0].applicationType").value("LEAVE"))
                .andExpect(jsonPath("$.data.typeDistribution[0].count").value(2))
                .andExpect(jsonPath("$.data.dailyTrend.length()").value(3))
                .andExpect(jsonPath("$.data.dailyTrend[0].date").value("2026-07-01"))
                .andExpect(jsonPath("$.data.dailyTrend[0].applicationCount").value(2));
    }

    private void insertApplication(
            long id,
            String applicationType,
            String applicationStatus,
            LocalDateTime createdAt
    ) {
        jdbcTemplate.update(
                "INSERT INTO flow_application (id, application_no, applicant_id, approver_id, "
                        + "application_type, start_time, end_time, reason, status, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id,
                "APP-" + id,
                100 + id,
                200 + id,
                applicationType,
                createdAt,
                createdAt.plusHours(8),
                "dashboard test",
                applicationStatus,
                createdAt,
                createdAt
        );
    }
}
