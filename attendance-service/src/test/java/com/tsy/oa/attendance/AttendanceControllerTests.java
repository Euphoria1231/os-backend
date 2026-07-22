package com.tsy.oa.attendance;

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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM attendance_record");
        redisGuard.clear();
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
    void exposesOpenApiDocument() throws Exception {
        mockMvc.perform(get("/v3/api-docs")
                        .header("X-Forwarded-Host", "localhost")
                        .header("X-Forwarded-Port", "8088")
                        .header("X-Forwarded-Proto", "http"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").isNotEmpty())
                .andExpect(jsonPath("$.servers[0].url").value("http://localhost:8088"))
                .andExpect(jsonPath("$.paths['/api/attendance/clock-in']").exists())
                .andExpect(jsonPath("$.paths['/api/attendance/calculations/daily'].post").exists())
                .andExpect(jsonPath("$.paths['/api/attendance/statistics/monthly'].get").exists())
                .andExpect(jsonPath("$.paths['/api/attendance/statistics/departments'].get").exists());
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
}
