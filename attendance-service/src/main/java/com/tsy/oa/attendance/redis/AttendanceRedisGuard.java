package com.tsy.oa.attendance.redis;

import java.time.Duration;
import java.time.LocalDate;

public interface AttendanceRedisGuard {

    LockToken tryLock(Long employeeId, LocalDate date, String operation, Duration ttl);

    boolean isCompleted(Long employeeId, LocalDate date, String operation);

    void markCompleted(Long employeeId, LocalDate date, String operation, Duration ttl);

    void unlock(LockToken lockToken);

    record LockToken(String key, String value) {
    }
}
