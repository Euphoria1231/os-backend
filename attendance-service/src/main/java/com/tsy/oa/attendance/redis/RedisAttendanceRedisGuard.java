package com.tsy.oa.attendance.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
public class RedisAttendanceRedisGuard implements AttendanceRedisGuard {

    private static final String LOCK_PREFIX = "attendance:lock:";
    private static final String COMPLETED_PREFIX = "attendance:done:";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    public RedisAttendanceRedisGuard(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public LockToken tryLock(Long employeeId, LocalDate date, String operation, Duration ttl) {
        String key = lockKey(employeeId, date, operation);
        String value = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, value, ttl);
        return Boolean.TRUE.equals(acquired) ? new LockToken(key, value) : null;
    }

    @Override
    public boolean isCompleted(Long employeeId, LocalDate date, String operation) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(completedKey(employeeId, date, operation)));
    }

    @Override
    public void markCompleted(Long employeeId, LocalDate date, String operation, Duration ttl) {
        redisTemplate.opsForValue().set(completedKey(employeeId, date, operation), "1", ttl);
    }

    @Override
    public void unlock(LockToken lockToken) {
        redisTemplate.execute(UNLOCK_SCRIPT, List.of(lockToken.key()), lockToken.value());
    }

    private String lockKey(Long employeeId, LocalDate date, String operation) {
        return LOCK_PREFIX + date + ":" + employeeId + ":" + operation;
    }

    private String completedKey(Long employeeId, LocalDate date, String operation) {
        return COMPLETED_PREFIX + date + ":" + employeeId + ":" + operation;
    }
}
