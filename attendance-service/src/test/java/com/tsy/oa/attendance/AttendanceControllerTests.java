package com.tsy.oa.attendance;

import com.tsy.oa.attendance.employee.EmployeeDirectory;
import com.tsy.oa.attendance.notification.PersonalNotificationPublisher;
import com.tsy.oa.attendance.redis.AttendanceRedisGuard;
import com.tsy.oa.attendance.redis.AttendanceRedisGuard.LockToken;
import com.tsy.oa.common.log.BusinessOperationLogger;
import com.tsy.oa.common.log.OperationLogCommand;
import com.tsy.oa.common.notification.PersonalNotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AttendanceControllerTests.TestApplication.class)
@AutoConfigureMockMvc
@Import(AttendanceControllerTests.AttendanceTestConfiguration.class)
class AttendanceControllerTests {

    private static final String EMPLOYEE_HEADER = "X-Employee-Id";
    private static final String CENTER_LOCATION =
            "{\"longitude\":119.411209,\"latitude\":26.022543}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MutableClock clock;

    @Autowired
    private InMemoryAttendanceRedisGuard redisGuard;

    @Autowired
    private InMemoryEmployeeDirectory employeeDirectory;

    @Autowired
    private RecordedOperationLogs operationLogs;

    @Autowired
    private RecordingPersonalNotificationPublisher notificationPublisher;

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM attendance_makeup_usage");
        jdbcTemplate.update("DELETE FROM attendance_makeup_quota");
        jdbcTemplate.update("DELETE FROM attendance_record");
        redisGuard.clear();
        employeeDirectory.clear();
        operationLogs.clear();
        notificationPublisher.clear();
        employeeDirectory.setLeader(10L, 20L);
        clock.setInstant(Instant.parse("2026-07-20T01:05:00Z"));
    }

    @Test
    void returnsConfiguredWorkPeriodsAndClockArea() throws Exception {
        mockMvc.perform(get("/api/attendance/clock-config")
                        .header(EMPLOYEE_HEADER, "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.morningStartTime").value("09:00"))
                .andExpect(jsonPath("$.data.morningEndTime").value("12:00"))
                .andExpect(jsonPath("$.data.afternoonStartTime").value("14:00"))
                .andExpect(jsonPath("$.data.afternoonEndTime").value("17:00"))
                .andExpect(jsonPath("$.data.centerLongitude").value(119.411209))
                .andExpect(jsonPath("$.data.centerLatitude").value(26.022543))
                .andExpect(jsonPath("$.data.radiusMeters").value(500));
    }

    @Test
    void acceptsClockInsideBoundaryAndRejectsClockOutsideBoundary() throws Exception {
        String outsideLocation =
                "{\"longitude\":119.411209,\"latitude\":26.027040501139}";
        mockMvc.perform(clockInRequest(
                        "10",
                        "{\"longitude\":119.411209,\"latitude\":26.027038702498}"
                ))
                .andExpect(status().isOk());

        mockMvc.perform(clockOutRequest("10", outsideLocation))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(42202));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendance_record "
                        + "WHERE employee_id = ? AND clock_out_time IS NOT NULL",
                Integer.class,
                10L
        ));

        mockMvc.perform(clockInRequest("11", outsideLocation))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(42202))
                .andExpect(jsonPath("$.message").value("当前位置不在允许打卡范围内"));

        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendance_record WHERE employee_id = ?",
                Integer.class,
                11L
        ));
    }

    @Test
    void rejectsClockWhenLocationIsMissingOrInvalid() throws Exception {
        mockMvc.perform(post("/api/attendance/clock-in")
                        .header(EMPLOYEE_HEADER, "10")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/attendance/clock-in")
                        .header(EMPLOYEE_HEADER, "10")
                        .contentType("application/json")
                        .content("{\"longitude\":181,\"latitude\":26.022543}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void clocksInAndOutAndReturnsTodayRecord() throws Exception {
        mockMvc.perform(clockInRequest("10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeId").value(10))
                .andExpect(jsonPath("$.data.attendanceDate").value("2026-07-20"))
                .andExpect(jsonPath("$.data.attendanceStatus").value("LATE"));

        clock.setInstant(Instant.parse("2026-07-20T10:00:00Z"));
        mockMvc.perform(clockOutRequest("10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clockOutTime").value("2026-07-20T18:00:00"));

        mockMvc.perform(get("/api/attendance/today").header(EMPLOYEE_HEADER, "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clockInTime").value("2026-07-20T09:05:00"))
                .andExpect(jsonPath("$.data.clockOutTime").value("2026-07-20T18:00:00"));

        assertEquals(1, operationLogs.count("CLOCK_IN", "SUCCESS"));
        assertEquals(1, operationLogs.count("CLOCK_OUT", "SUCCESS"));
        Long recordId = recordId(10L);
        assertEquals(List.of(
                "attendance:" + recordId + ":late:10:ATTENDANCE_ABNORMAL"
        ), notificationPublisher.summaries());
    }

    @Test
    void marksClockOutBeforeWorkEndAsEarlyLeave() throws Exception {
        clock.setInstant(Instant.parse("2026-07-20T00:55:00Z"));
        mockMvc.perform(clockInRequest("10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attendanceStatus").value("NORMAL"));

        clock.setInstant(Instant.parse("2026-07-20T08:30:00Z"));
        mockMvc.perform(clockOutRequest("10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attendanceStatus").value("EARLY_LEAVE"));

        Long recordId = recordId(10L);
        assertEquals(List.of(
                "attendance:" + recordId + ":early-leave:10:ATTENDANCE_ABNORMAL"
        ), notificationPublisher.summaries());
    }

    @Test
    void rollsBackLateClockInWhenNotificationPublishingFails() throws Exception {
        notificationPublisher.failNext();

        mockMvc.perform(clockInRequest("10"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(50000));

        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendance_record WHERE employee_id = ?",
                Integer.class,
                10L
        ));
        assertFalse(redisGuard.isCompleted(
                10L, LocalDate.of(2026, 7, 20), "CLOCK_IN"
        ));

        mockMvc.perform(clockInRequest("10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attendanceStatus").value("LATE"));
    }

    @Test
    void doesNotNotifyForNormalClockInAndOut() throws Exception {
        clock.setInstant(Instant.parse("2026-07-20T00:55:00Z"));
        mockMvc.perform(clockInRequest("10"))
                .andExpect(status().isOk());

        clock.setInstant(Instant.parse("2026-07-20T10:00:00Z"));
        mockMvc.perform(clockOutRequest("10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attendanceStatus").value("NORMAL"));

        assertEquals(List.of(), notificationPublisher.summaries());
    }

    @Test
    void rollsBackEarlyClockOutWhenNotificationPublishingFails() throws Exception {
        clock.setInstant(Instant.parse("2026-07-20T00:55:00Z"));
        mockMvc.perform(clockInRequest("10"))
                .andExpect(status().isOk());

        clock.setInstant(Instant.parse("2026-07-20T08:30:00Z"));
        notificationPublisher.failNext();
        mockMvc.perform(clockOutRequest("10"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(50000));

        assertNull(jdbcTemplate.queryForObject(
                "SELECT clock_out_time FROM attendance_record WHERE employee_id = ?",
                LocalDateTime.class,
                10L
        ));
        assertEquals("NORMAL", jdbcTemplate.queryForObject(
                "SELECT attendance_status FROM attendance_record WHERE employee_id = ?",
                String.class,
                10L
        ));
        assertFalse(redisGuard.isCompleted(
                10L, LocalDate.of(2026, 7, 20), "CLOCK_OUT"
        ));

        mockMvc.perform(clockOutRequest("10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attendanceStatus").value("EARLY_LEAVE"));
    }

    @Test
    void keepsLateStatusWhenClockOutIsAlsoEarly() throws Exception {
        mockMvc.perform(clockInRequest("10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attendanceStatus").value("LATE"));

        clock.setInstant(Instant.parse("2026-07-20T08:30:00Z"));
        mockMvc.perform(clockOutRequest("10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attendanceStatus").value("LATE"));

        Long recordId = recordId(10L);
        assertEquals(List.of(
                "attendance:" + recordId + ":late:10:ATTENDANCE_ABNORMAL",
                "attendance:" + recordId + ":early-leave:10:ATTENDANCE_ABNORMAL"
        ), notificationPublisher.summaries());
    }

    @Test
    void rejectsInvalidClockSequenceAndDuplicateClockIn() throws Exception {
        mockMvc.perform(clockOutRequest("10"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40903));

        mockMvc.perform(clockInRequest("10"))
                .andExpect(status().isOk());
        mockMvc.perform(clockInRequest("10"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40901));

        assertEquals(1, operationLogs.count("CLOCK_OUT", "FAILURE"));
        assertEquals(1, operationLogs.count("CLOCK_IN", "SUCCESS"));
        assertEquals(1, operationLogs.count("CLOCK_IN", "FAILURE"));
    }

    @Test
    void rejectsClockWhenDistributedLockIsBusy() throws Exception {
        redisGuard.block(10L, LocalDate.of(2026, 7, 20), "CLOCK_IN");

        mockMvc.perform(clockInRequest("10"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40904));
    }

    @Test
    void queriesPersonalRecordsByDateRange() throws Exception {
        mockMvc.perform(clockInRequest("10"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/attendance/records")
                        .header(EMPLOYEE_HEADER, "10")
                        .param("startDate", "2026-07-01")
                        .param("endDate", "2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].employeeId").value(10));
    }

    @Test
    void directLeaderAssignsMonthlyMakeupQuotaAndEmployeeQueriesBalance() throws Exception {
        mockMvc.perform(put("/api/attendance/makeup-quotas/{employeeId}", 10)
                        .header(EMPLOYEE_HEADER, "20")
                        .contentType("application/json")
                        .content("{\"quotaMonth\":\"2026-07\",\"totalCount\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeId").value(10))
                .andExpect(jsonPath("$.data.quotaMonth").value("2026-07"))
                .andExpect(jsonPath("$.data.totalCount").value(5))
                .andExpect(jsonPath("$.data.usedCount").value(0))
                .andExpect(jsonPath("$.data.remainingCount").value(5))
                .andExpect(jsonPath("$.data.assignedBy").value(20));

        mockMvc.perform(get("/api/attendance/makeup-quotas/mine")
                        .header(EMPLOYEE_HEADER, "10")
                        .param("quotaMonth", "2026-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(5))
                .andExpect(jsonPath("$.data.usedCount").value(0))
                .andExpect(jsonPath("$.data.remainingCount").value(5));
    }

    @Test
    void rejectsMakeupQuotaAssignmentByNonDirectLeader() throws Exception {
        mockMvc.perform(put("/api/attendance/makeup-quotas/{employeeId}", 10)
                        .header(EMPLOYEE_HEADER, "30")
                        .contentType("application/json")
                        .content("{\"quotaMonth\":\"2026-07\",\"totalCount\":5}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    @Test
    void rejectsMakeupQuotaBelowAlreadyUsedCount() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO attendance_makeup_quota "
                        + "(employee_id, quota_month, total_count, used_count, assigned_by) "
                        + "VALUES (?, ?, ?, ?, ?)",
                10L, LocalDate.of(2026, 7, 1), 5, 3, 20L
        );

        mockMvc.perform(put("/api/attendance/makeup-quotas/{employeeId}", 10)
                        .header(EMPLOYEE_HEADER, "20")
                        .contentType("application/json")
                        .content("{\"quotaMonth\":\"2026-07\",\"totalCount\":2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40905));
    }

    @Test
    void rejectsNonPositiveMakeupQuota() throws Exception {
        mockMvc.perform(put("/api/attendance/makeup-quotas/{employeeId}", 10)
                        .header(EMPLOYEE_HEADER, "20")
                        .contentType("application/json")
                        .content("{\"quotaMonth\":\"2026-07\",\"totalCount\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void returnsMakeupEligibilityForLateRecordWithRemainingQuota() throws Exception {
        mockMvc.perform(clockInRequest("10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attendanceStatus").value("LATE"));
        Long attendanceRecordId = jdbcTemplate.queryForObject(
                "SELECT id FROM attendance_record WHERE employee_id = ?",
                Long.class,
                10L
        );
        mockMvc.perform(put("/api/attendance/makeup-quotas/{employeeId}", 10)
                        .header(EMPLOYEE_HEADER, "20")
                        .contentType("application/json")
                        .content("{\"quotaMonth\":\"2026-07\",\"totalCount\":5}"))
                .andExpect(status().isOk());

        mockMvc.perform(get(
                        "/internal/attendance/records/{recordId}/makeup-eligibility",
                        attendanceRecordId
                ).param("employeeId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eligible").value(true))
                .andExpect(jsonPath("$.data.attendanceRecordId").value(attendanceRecordId))
                .andExpect(jsonPath("$.data.employeeId").value(10))
                .andExpect(jsonPath("$.data.attendanceDate").value("2026-07-20"))
                .andExpect(jsonPath("$.data.remainingCount").value(5));
    }

    @Test
    void rejectsMakeupEligibilityForAnotherEmployeesRecord() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO attendance_record "
                        + "(employee_id, attendance_date, clock_in_time, attendance_status) "
                        + "VALUES (?, ?, ?, ?)",
                11L,
                LocalDate.of(2026, 7, 20),
                java.time.LocalDateTime.of(2026, 7, 20, 9, 5),
                "LATE"
        );
        Long attendanceRecordId = jdbcTemplate.queryForObject(
                "SELECT id FROM attendance_record WHERE employee_id = ?",
                Long.class,
                11L
        );
        mockMvc.perform(put("/api/attendance/makeup-quotas/{employeeId}", 10)
                        .header(EMPLOYEE_HEADER, "20")
                        .contentType("application/json")
                        .content("{\"quotaMonth\":\"2026-07\",\"totalCount\":5}"))
                .andExpect(status().isOk());

        mockMvc.perform(get(
                        "/internal/attendance/records/{recordId}/makeup-eligibility",
                        attendanceRecordId
                ).param("employeeId", "10"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void rejectsMakeupEligibilityForNonLateRecord() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO attendance_record "
                        + "(employee_id, attendance_date, clock_in_time, attendance_status) "
                        + "VALUES (?, ?, ?, ?)",
                10L,
                LocalDate.of(2026, 7, 20),
                java.time.LocalDateTime.of(2026, 7, 20, 9, 0),
                "NORMAL"
        );
        Long attendanceRecordId = jdbcTemplate.queryForObject(
                "SELECT id FROM attendance_record WHERE employee_id = ?",
                Long.class,
                10L
        );
        mockMvc.perform(put("/api/attendance/makeup-quotas/{employeeId}", 10)
                        .header(EMPLOYEE_HEADER, "20")
                        .contentType("application/json")
                        .content("{\"quotaMonth\":\"2026-07\",\"totalCount\":5}"))
                .andExpect(status().isOk());

        mockMvc.perform(get(
                        "/internal/attendance/records/{recordId}/makeup-eligibility",
                        attendanceRecordId
                ).param("employeeId", "10"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(42201));
    }

    @Test
    void rejectsMakeupEligibilityWhenMonthlyQuotaIsExhausted() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO attendance_record "
                        + "(employee_id, attendance_date, clock_in_time, attendance_status) "
                        + "VALUES (?, ?, ?, ?)",
                10L,
                LocalDate.of(2026, 7, 20),
                java.time.LocalDateTime.of(2026, 7, 20, 9, 5),
                "LATE"
        );
        Long attendanceRecordId = jdbcTemplate.queryForObject(
                "SELECT id FROM attendance_record WHERE employee_id = ?",
                Long.class,
                10L
        );
        jdbcTemplate.update(
                "INSERT INTO attendance_makeup_quota "
                        + "(employee_id, quota_month, total_count, used_count, assigned_by) "
                        + "VALUES (?, ?, ?, ?, ?)",
                10L, LocalDate.of(2026, 7, 1), 3, 3, 20L
        );

        mockMvc.perform(get(
                        "/internal/attendance/records/{recordId}/makeup-eligibility",
                        attendanceRecordId
                ).param("employeeId", "10"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40906));
    }

    @Test
    void rejectsMakeupEligibilityWhenMonthlyQuotaIsNotAssigned() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO attendance_record "
                        + "(employee_id, attendance_date, clock_in_time, attendance_status) "
                        + "VALUES (?, ?, ?, ?)",
                10L,
                LocalDate.of(2026, 7, 20),
                java.time.LocalDateTime.of(2026, 7, 20, 9, 5),
                "LATE"
        );
        Long attendanceRecordId = jdbcTemplate.queryForObject(
                "SELECT id FROM attendance_record WHERE employee_id = ?",
                Long.class,
                10L
        );

        mockMvc.perform(get(
                        "/internal/attendance/records/{recordId}/makeup-eligibility",
                        attendanceRecordId
                ).param("employeeId", "10"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40906));
    }

    @Test
    void completesApprovedMakeupAndConsumesMonthlyQuota() throws Exception {
        Long attendanceRecordId = insertLateRecordWithQuota(5, 0);

        mockMvc.perform(post(
                        "/internal/attendance/records/{recordId}/makeup-completion",
                        attendanceRecordId
                ).contentType("application/json")
                        .content("{\"employeeId\":10,\"applicationId\":1001}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applicationId").value(1001))
                .andExpect(jsonPath("$.data.attendanceRecordId").value(attendanceRecordId))
                .andExpect(jsonPath("$.data.attendanceStatus").value("MAKEUP"))
                .andExpect(jsonPath("$.data.originalAttendanceStatus").value("LATE"))
                .andExpect(jsonPath("$.data.usedCount").value(1))
                .andExpect(jsonPath("$.data.remainingCount").value(4));

        assertEquals("MAKEUP", jdbcTemplate.queryForObject(
                "SELECT attendance_status FROM attendance_record WHERE id = ?",
                String.class,
                attendanceRecordId
        ));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT used_count FROM attendance_makeup_quota WHERE employee_id = ?",
                Integer.class,
                10L
        ));
    }

    @Test
    void treatsRepeatedMakeupCompletionAsIdempotent() throws Exception {
        Long attendanceRecordId = insertLateRecordWithQuota(5, 0);
        String requestBody = "{\"employeeId\":10,\"applicationId\":1001}";

        mockMvc.perform(post(
                        "/internal/attendance/records/{recordId}/makeup-completion",
                        attendanceRecordId
                ).contentType("application/json").content(requestBody))
                .andExpect(status().isOk());
        mockMvc.perform(post(
                        "/internal/attendance/records/{recordId}/makeup-completion",
                        attendanceRecordId
                ).contentType("application/json").content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.usedCount").value(1))
                .andExpect(jsonPath("$.data.remainingCount").value(4));

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT used_count FROM attendance_makeup_quota WHERE employee_id = ?",
                Integer.class,
                10L
        ));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendance_makeup_usage WHERE application_id = ?",
                Integer.class,
                1001L
        ));
    }

    @Test
    void rejectsMakeupCompletionWhenQuotaWasExhaustedBeforeApproval() throws Exception {
        Long attendanceRecordId = insertLateRecordWithQuota(1, 1);

        mockMvc.perform(post(
                        "/internal/attendance/records/{recordId}/makeup-completion",
                        attendanceRecordId
                ).contentType("application/json")
                        .content("{\"employeeId\":10,\"applicationId\":1001}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40906));

        assertEquals("LATE", jdbcTemplate.queryForObject(
                "SELECT attendance_status FROM attendance_record WHERE id = ?",
                String.class,
                attendanceRecordId
        ));
    }

    @Test
    void exposesOpenApiDocument() throws Exception {
        mockMvc.perform(get("/v3/api-docs")
                        .header("X-Forwarded-Host", "localhost")
                        .header("X-Forwarded-Port", "8088")
                        .header("X-Forwarded-Proto", "http"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").isNotEmpty())
                .andExpect(jsonPath("$.servers[0].url").value("http://localhost:8088"))
                .andExpect(jsonPath("$.paths['/api/attendance/clock-in']").exists());
    }

    private MockHttpServletRequestBuilder clockInRequest(String employeeId) {
        return clockInRequest(employeeId, CENTER_LOCATION);
    }

    private MockHttpServletRequestBuilder clockInRequest(
            String employeeId,
            String location
    ) {
        return post("/api/attendance/clock-in")
                .header(EMPLOYEE_HEADER, employeeId)
                .contentType("application/json")
                .content(location);
    }

    private MockHttpServletRequestBuilder clockOutRequest(String employeeId) {
        return clockOutRequest(employeeId, CENTER_LOCATION);
    }

    private MockHttpServletRequestBuilder clockOutRequest(
            String employeeId,
            String location
    ) {
        return post("/api/attendance/clock-out")
                .header(EMPLOYEE_HEADER, employeeId)
                .contentType("application/json")
                .content(location);
    }

    private Long insertLateRecordWithQuota(int totalCount, int usedCount) {
        jdbcTemplate.update(
                "INSERT INTO attendance_record "
                        + "(employee_id, attendance_date, clock_in_time, attendance_status) "
                        + "VALUES (?, ?, ?, ?)",
                10L,
                LocalDate.of(2026, 7, 20),
                java.time.LocalDateTime.of(2026, 7, 20, 9, 5),
                "LATE"
        );
        jdbcTemplate.update(
                "INSERT INTO attendance_makeup_quota "
                        + "(employee_id, quota_month, total_count, used_count, assigned_by) "
                        + "VALUES (?, ?, ?, ?, ?)",
                10L, LocalDate.of(2026, 7, 1), totalCount, usedCount, 20L
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM attendance_record WHERE employee_id = ?",
                Long.class,
                10L
        );
    }

    private Long recordId(Long employeeId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM attendance_record WHERE employee_id = ?",
                Long.class,
                employeeId
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan("com.tsy.oa.attendance")
    static class TestApplication {
    }

    @TestConfiguration
    static class AttendanceTestConfiguration {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(ZoneId.of("Asia/Shanghai"));
        }

        @Bean
        @Primary
        InMemoryAttendanceRedisGuard inMemoryAttendanceRedisGuard() {
            return new InMemoryAttendanceRedisGuard();
        }

        @Bean
        @Primary
        InMemoryEmployeeDirectory inMemoryEmployeeDirectory() {
            return new InMemoryEmployeeDirectory();
        }

        @Bean
        RecordedOperationLogs recordedOperationLogs() {
            return new RecordedOperationLogs();
        }

        @Bean
        @Primary
        BusinessOperationLogger testBusinessOperationLogger(RecordedOperationLogs logs) {
            return new BusinessOperationLogger(
                    "attendance-service", logs::add, (command, exception) -> {
                    }
            );
        }

        @Bean
        @Primary
        RecordingPersonalNotificationPublisher recordingPersonalNotificationPublisher() {
            return new RecordingPersonalNotificationPublisher();
        }
    }

    static class MutableClock extends Clock {

        private final ZoneId zoneId;
        private volatile Instant instant = Instant.EPOCH;

        MutableClock(ZoneId zoneId) {
            this.zoneId = zoneId;
        }

        void setInstant(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return zoneId;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    static class InMemoryAttendanceRedisGuard implements AttendanceRedisGuard {

        private final Set<String> lockedKeys = ConcurrentHashMap.newKeySet();
        private final Set<String> completedKeys = ConcurrentHashMap.newKeySet();

        @Override
        public LockToken tryLock(Long employeeId, LocalDate date, String operation, Duration ttl) {
            String key = key(employeeId, date, operation);
            if (!lockedKeys.add(key)) {
                return null;
            }
            return new LockToken(key, UUID.randomUUID().toString());
        }

        @Override
        public boolean isCompleted(Long employeeId, LocalDate date, String operation) {
            return completedKeys.contains(key(employeeId, date, operation));
        }

        @Override
        public void markCompleted(Long employeeId, LocalDate date, String operation, Duration ttl) {
            completedKeys.add(key(employeeId, date, operation));
        }

        @Override
        public void unlock(LockToken lockToken) {
            lockedKeys.remove(lockToken.key());
        }

        void block(Long employeeId, LocalDate date, String operation) {
            lockedKeys.add(key(employeeId, date, operation));
        }

        void clear() {
            lockedKeys.clear();
            completedKeys.clear();
        }

        private String key(Long employeeId, LocalDate date, String operation) {
            return employeeId + ":" + date + ":" + operation;
        }
    }

    static class InMemoryEmployeeDirectory implements EmployeeDirectory {

        private final Map<Long, Long> leaders = new ConcurrentHashMap<>();

        @Override
        public Long findDirectLeaderId(Long employeeId) {
            return leaders.get(employeeId);
        }

        void setLeader(Long employeeId, Long leaderId) {
            leaders.put(employeeId, leaderId);
        }

        void clear() {
            leaders.clear();
        }
    }

    static class RecordedOperationLogs {

        private final List<OperationLogCommand> commands = new java.util.concurrent.CopyOnWriteArrayList<>();

        void add(OperationLogCommand command) {
            commands.add(command);
        }

        long count(String operationType, String status) {
            return commands.stream()
                    .filter(command -> operationType.equals(command.operationType()))
                    .filter(command -> status.equals(command.operationStatus()))
                    .count();
        }

        void clear() {
            commands.clear();
        }
    }

    static class RecordingPersonalNotificationPublisher implements PersonalNotificationPublisher {

        private final List<PersonalNotificationEvent> events = new java.util.concurrent.CopyOnWriteArrayList<>();
        private boolean failNext;

        @Override
        public void publish(PersonalNotificationEvent event) {
            if (failNext) {
                failNext = false;
                throw new IllegalStateException("RocketMQ unavailable");
            }
            events.add(event);
        }

        List<String> summaries() {
            return events.stream()
                    .map(event -> event.eventId()
                            + ":" + event.recipientEmployeeId()
                            + ":" + event.notificationType())
                    .toList();
        }

        void failNext() {
            failNext = true;
        }

        void clear() {
            events.clear();
            failNext = false;
        }
    }
}
