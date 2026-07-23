package com.tsy.oa.attendance.service;

import com.tsy.oa.attendance.config.AttendanceClockProperties;
import com.tsy.oa.attendance.dto.AttendanceRecordResponse;
import com.tsy.oa.attendance.dto.MakeupQuotaAssignmentRequest;
import com.tsy.oa.attendance.dto.MakeupEligibilityResponse;
import com.tsy.oa.attendance.dto.MakeupQuotaResponse;
import com.tsy.oa.attendance.employee.EmployeeDirectory;
import com.tsy.oa.attendance.error.AttendanceErrorCode;
import com.tsy.oa.attendance.mapper.AttendanceMapper;
import com.tsy.oa.attendance.model.AttendanceRecord;
import com.tsy.oa.attendance.model.AttendanceMakeupQuota;
import com.tsy.oa.attendance.redis.AttendanceRedisGuard;
import com.tsy.oa.attendance.redis.AttendanceRedisGuard.LockToken;
import com.tsy.oa.common.error.CommonErrorCode;
import com.tsy.oa.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
public class AttendanceService {

    private static final String CLOCK_IN = "CLOCK_IN";
    private static final String CLOCK_OUT = "CLOCK_OUT";

    private final AttendanceMapper attendanceMapper;
    private final AttendanceRedisGuard redisGuard;
    private final AttendanceClockProperties clockProperties;
    private final EmployeeDirectory employeeDirectory;
    private final Clock clock;

    public AttendanceService(
            AttendanceMapper attendanceMapper,
            AttendanceRedisGuard redisGuard,
            AttendanceClockProperties clockProperties,
            EmployeeDirectory employeeDirectory,
            Clock clock
    ) {
        this.attendanceMapper = attendanceMapper;
        this.redisGuard = redisGuard;
        this.clockProperties = clockProperties;
        this.employeeDirectory = employeeDirectory;
        this.clock = clock;
    }

    @Transactional
    public AttendanceRecordResponse clockIn(Long employeeId) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate date = now.toLocalDate();
        if (redisGuard.isCompleted(employeeId, date, CLOCK_IN)) {
            throw new BusinessException(AttendanceErrorCode.ALREADY_CLOCKED_IN);
        }

        LockToken lockToken = acquireLock(employeeId, date, CLOCK_IN);
        try {
            AttendanceRecord existing = attendanceMapper.findByEmployeeAndDate(employeeId, date);
            if (existing != null && existing.getClockInTime() != null) {
                throw new BusinessException(AttendanceErrorCode.ALREADY_CLOCKED_IN);
            }

            AttendanceRecord record = new AttendanceRecord();
            record.setEmployeeId(employeeId);
            record.setAttendanceDate(date);
            record.setClockInTime(now);
            record.setAttendanceStatus(resolveClockInStatus(now));
            attendanceMapper.insert(record);
            redisGuard.markCompleted(
                    employeeId, date, CLOCK_IN, clockProperties.getCompletedMarkerTtl()
            );
            return getToday(employeeId);
        } finally {
            redisGuard.unlock(lockToken);
        }
    }

    @Transactional
    public AttendanceRecordResponse clockOut(Long employeeId) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate date = now.toLocalDate();
        if (redisGuard.isCompleted(employeeId, date, CLOCK_OUT)) {
            throw new BusinessException(AttendanceErrorCode.ALREADY_CLOCKED_OUT);
        }

        LockToken lockToken = acquireLock(employeeId, date, CLOCK_OUT);
        try {
            AttendanceRecord record = attendanceMapper.findByEmployeeAndDate(employeeId, date);
            if (record == null || record.getClockInTime() == null) {
                throw new BusinessException(AttendanceErrorCode.CLOCK_IN_REQUIRED);
            }
            if (record.getClockOutTime() != null) {
                throw new BusinessException(AttendanceErrorCode.ALREADY_CLOCKED_OUT);
            }
            attendanceMapper.updateClockOut(record.getId(), now);
            redisGuard.markCompleted(
                    employeeId, date, CLOCK_OUT, clockProperties.getCompletedMarkerTtl()
            );
            return getToday(employeeId);
        } finally {
            redisGuard.unlock(lockToken);
        }
    }

    @Transactional(readOnly = true)
    public AttendanceRecordResponse getToday(Long employeeId) {
        AttendanceRecord record = attendanceMapper.findByEmployeeAndDate(
                employeeId, LocalDate.now(clock)
        );
        if (record == null) {
            throw new BusinessException(AttendanceErrorCode.RECORD_NOT_FOUND);
        }
        return AttendanceRecordResponse.from(record);
    }

    @Transactional(readOnly = true)
    public List<AttendanceRecordResponse> listRecords(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST);
        }
        return attendanceMapper.findByEmployeeAndDateRange(employeeId, startDate, endDate)
                .stream()
                .map(AttendanceRecordResponse::from)
                .toList();
    }

    @Transactional
    public MakeupQuotaResponse assignMakeupQuota(
            Long leaderId,
            Long employeeId,
            MakeupQuotaAssignmentRequest request
    ) {
        if (!leaderId.equals(employeeDirectory.findDirectLeaderId(employeeId))) {
            throw new BusinessException(AttendanceErrorCode.NOT_DIRECT_LEADER);
        }
        LocalDate quotaMonth = request.quotaMonth().atDay(1);
        AttendanceMakeupQuota existing = attendanceMapper.findMakeupQuota(
                employeeId, quotaMonth
        );
        if (existing != null && request.totalCount() < existing.getUsedCount()) {
            throw new BusinessException(AttendanceErrorCode.MAKEUP_QUOTA_BELOW_USED);
        }
        AttendanceMakeupQuota quota = new AttendanceMakeupQuota();
        quota.setEmployeeId(employeeId);
        quota.setQuotaMonth(quotaMonth);
        quota.setTotalCount(request.totalCount());
        quota.setUsedCount(0);
        quota.setAssignedBy(leaderId);
        attendanceMapper.upsertMakeupQuota(quota);
        return getMakeupQuota(employeeId, request.quotaMonth());
    }

    @Transactional(readOnly = true)
    public MakeupQuotaResponse getMakeupQuota(Long employeeId, YearMonth quotaMonth) {
        AttendanceMakeupQuota quota = attendanceMapper.findMakeupQuota(
                employeeId, quotaMonth.atDay(1)
        );
        if (quota == null) {
            throw new BusinessException(AttendanceErrorCode.RECORD_NOT_FOUND);
        }
        return MakeupQuotaResponse.from(quota);
    }

    @Transactional(readOnly = true)
    public MakeupEligibilityResponse getMakeupEligibility(Long recordId, Long employeeId) {
        AttendanceRecord record = attendanceMapper.findById(recordId);
        if (record == null || !record.getEmployeeId().equals(employeeId)) {
            throw new BusinessException(AttendanceErrorCode.RECORD_NOT_FOUND);
        }
        if (!"LATE".equals(record.getAttendanceStatus())) {
            throw new BusinessException(AttendanceErrorCode.MAKEUP_REQUIRES_LATE_RECORD);
        }
        AttendanceMakeupQuota quota = attendanceMapper.findMakeupQuota(
                employeeId, YearMonth.from(record.getAttendanceDate()).atDay(1)
        );
        if (quota == null || quota.getTotalCount() <= quota.getUsedCount()) {
            throw new BusinessException(AttendanceErrorCode.MAKEUP_QUOTA_UNAVAILABLE);
        }
        return MakeupEligibilityResponse.eligible(record, quota);
    }

    private LockToken acquireLock(Long employeeId, LocalDate date, String operation) {
        LockToken lockToken = redisGuard.tryLock(
                employeeId, date, operation, clockProperties.getLockTtl()
        );
        if (lockToken == null) {
            throw new BusinessException(AttendanceErrorCode.OPERATION_BUSY);
        }
        return lockToken;
    }

    private String resolveClockInStatus(LocalDateTime now) {
        boolean late = now.toLocalTime().isAfter(
                clockProperties.getWorkStartTime().plusMinutes(clockProperties.getLateThresholdMinutes())
        );
        return late ? "LATE" : "NORMAL";
    }
}
