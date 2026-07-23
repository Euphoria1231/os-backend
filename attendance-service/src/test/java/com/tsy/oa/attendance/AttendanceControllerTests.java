package com.tsy.oa.attendance;

import com.tsy.oa.attendance.employee.EmployeeDirectory;
import com.tsy.oa.attendance.redis.AttendanceRedisGuard;
import com.tsy.oa.attendance.redis.AttendanceRedisGuard.LockToken;
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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM attendance_makeup_quota");
        jdbcTemplate.update("DELETE FROM attendance_record");
        redisGuard.clear();
        employeeDirectory.clear();
        employeeDirectory.setLeader(10L, 20L);
        clock.setInstant(Instant.parse("2026-07-20T01:05:00Z"));
    }

    @Test
    void clocksInAndOutAndReturnsTodayRecord() throws Exception {
        mockMvc.perform(post("/api/attendance/clock-in").header(EMPLOYEE_HEADER, "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeId").value(10))
                .andExpect(jsonPath("$.data.attendanceDate").value("2026-07-20"))
                .andExpect(jsonPath("$.data.attendanceStatus").value("LATE"));

        clock.setInstant(Instant.parse("2026-07-20T10:00:00Z"));
        mockMvc.perform(post("/api/attendance/clock-out").header(EMPLOYEE_HEADER, "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clockOutTime").value("2026-07-20T18:00:00"));

        mockMvc.perform(get("/api/attendance/today").header(EMPLOYEE_HEADER, "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clockInTime").value("2026-07-20T09:05:00"))
                .andExpect(jsonPath("$.data.clockOutTime").value("2026-07-20T18:00:00"));
    }

    @Test
    void rejectsInvalidClockSequenceAndDuplicateClockIn() throws Exception {
        mockMvc.perform(post("/api/attendance/clock-out").header(EMPLOYEE_HEADER, "10"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40903));

        mockMvc.perform(post("/api/attendance/clock-in").header(EMPLOYEE_HEADER, "10"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/attendance/clock-in").header(EMPLOYEE_HEADER, "10"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40901));
    }

    @Test
    void rejectsClockWhenDistributedLockIsBusy() throws Exception {
        redisGuard.block(10L, LocalDate.of(2026, 7, 20), "CLOCK_IN");

        mockMvc.perform(post("/api/attendance/clock-in").header(EMPLOYEE_HEADER, "10"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40904));
    }

    @Test
    void queriesPersonalRecordsByDateRange() throws Exception {
        mockMvc.perform(post("/api/attendance/clock-in").header(EMPLOYEE_HEADER, "10"))
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
        mockMvc.perform(post("/api/attendance/clock-in").header(EMPLOYEE_HEADER, "10"))
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
}
