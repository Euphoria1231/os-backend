package com.tsy.oa.attendance.service;

import com.tsy.oa.attendance.config.AttendanceClockProperties;
import com.tsy.oa.attendance.config.AttendanceGeofenceProperties;
import com.tsy.oa.attendance.dto.AttendanceClockConfigResponse;
import com.tsy.oa.attendance.dto.AttendanceRecordResponse;
import com.tsy.oa.attendance.dto.ClockLocationRequest;
import com.tsy.oa.attendance.dto.MakeupCompletionRequest;
import com.tsy.oa.attendance.dto.MakeupCompletionResponse;
import com.tsy.oa.attendance.dto.MakeupEligibilityResponse;
import com.tsy.oa.attendance.dto.MakeupQuotaAssignmentRequest;
import com.tsy.oa.attendance.dto.MakeupQuotaResponse;
import com.tsy.oa.attendance.employee.EmployeeDirectory;
import com.tsy.oa.attendance.error.AttendanceErrorCode;
import com.tsy.oa.attendance.mapper.AttendanceMapper;
import com.tsy.oa.attendance.model.AttendanceRecord;
import com.tsy.oa.attendance.model.AttendanceMakeupQuota;
import com.tsy.oa.attendance.model.AttendanceMakeupUsage;
import com.tsy.oa.attendance.notification.PersonalNotificationPublisher;
import com.tsy.oa.attendance.redis.AttendanceRedisGuard;
import com.tsy.oa.attendance.redis.AttendanceRedisGuard.LockToken;
import com.tsy.oa.common.error.CommonErrorCode;
import com.tsy.oa.common.exception.BusinessException;
import com.tsy.oa.common.notification.PersonalNotificationEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

@Service
public class AttendanceService {

    private static final String CLOCK_IN = "CLOCK_IN";
    private static final String CLOCK_OUT = "CLOCK_OUT";
    private static final String NORMAL = "NORMAL";
    private static final String LATE = "LATE";
    private static final String ABSENT = "ABSENT";

    private final AttendanceMapper attendanceMapper;
    private final AttendanceRedisGuard redisGuard;
    private final AttendanceClockProperties clockProperties;
    private final AttendanceGeofenceProperties geofenceProperties;
    private final AttendanceGeofenceService geofenceService;
    private final EmployeeDirectory employeeDirectory;
    private final PersonalNotificationPublisher notificationPublisher;
    private final Clock clock;

    public AttendanceService(
            AttendanceMapper attendanceMapper,
            AttendanceRedisGuard redisGuard,
            AttendanceClockProperties clockProperties,
            AttendanceGeofenceProperties geofenceProperties,
            AttendanceGeofenceService geofenceService,
            EmployeeDirectory employeeDirectory,
            PersonalNotificationPublisher notificationPublisher,
            Clock clock
    ) {
        this.attendanceMapper = attendanceMapper;
        this.redisGuard = redisGuard;
        this.clockProperties = clockProperties;
        this.geofenceProperties = geofenceProperties;
        this.geofenceService = geofenceService;
        this.employeeDirectory = employeeDirectory;
        this.notificationPublisher = notificationPublisher;
        this.clock = clock;
    }

    @Transactional
    public AttendanceRecordResponse clockIn(
            Long employeeId,
            ClockLocationRequest location
    ) {
        geofenceService.requireInside(location);
        LocalDateTime now = LocalDateTime.now(clock);
        if (!now.toLocalTime().isBefore(clockProperties.getAfternoonClockStartTime())) {
            throw new BusinessException(AttendanceErrorCode.MORNING_CLOCK_CLOSED);
        }
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
            if (LATE.equals(record.getAttendanceStatus())) {
                publishAttendanceAbnormal(
                        record.getId(), employeeId, "morning-late", "迟到", "上午", now
                );
            }
            redisGuard.markCompleted(
                    employeeId, date, CLOCK_IN, clockProperties.getCompletedMarkerTtl()
            );
            return getToday(employeeId);
        } finally {
            redisGuard.unlock(lockToken);
        }
    }

    @Transactional
    public AttendanceRecordResponse clockOut(
            Long employeeId,
            ClockLocationRequest location
    ) {
        geofenceService.requireInside(location);
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.toLocalTime().isBefore(clockProperties.getAfternoonClockStartTime())) {
            throw new BusinessException(AttendanceErrorCode.AFTERNOON_CLOCK_NOT_OPEN);
        }
        LocalDate date = now.toLocalDate();
        if (redisGuard.isCompleted(employeeId, date, CLOCK_OUT)) {
            throw new BusinessException(AttendanceErrorCode.ALREADY_CLOCKED_OUT);
        }

        LockToken lockToken = acquireLock(employeeId, date, CLOCK_OUT);
        try {
            AttendanceRecord record = attendanceMapper.findByEmployeeAndDate(employeeId, date);
            String afternoonStatus = resolveClockStatus(
                    now, clockProperties.getAfternoonStartTime()
            );
            if (record == null) {
                record = new AttendanceRecord();
                record.setEmployeeId(employeeId);
                record.setAttendanceDate(date);
                record.setClockOutTime(now);
                record.setAttendanceStatus(ABSENT);
                attendanceMapper.insert(record);
            } else {
                if (record.getClockOutTime() != null) {
                    throw new BusinessException(AttendanceErrorCode.ALREADY_CLOCKED_OUT);
                }
                attendanceMapper.updateClockOut(
                        record.getId(),
                        now,
                        resolveDailyStatus(record.getAttendanceStatus(), afternoonStatus)
                );
            }
            if (LATE.equals(afternoonStatus)) {
                publishAttendanceAbnormal(
                        record.getId(), employeeId, "afternoon-late", "迟到", "下午", now
                );
            }
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

    public AttendanceClockConfigResponse getClockConfig() {
        return AttendanceClockConfigResponse.from(clockProperties, geofenceProperties);
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

    @Transactional(readOnly = true)
    public List<AttendanceRecordResponse> listDirectReportRecords(
            Long requesterId,
            Long targetEmployeeId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (requesterId.equals(targetEmployeeId)
                || !requesterId.equals(employeeDirectory.findDirectLeaderId(targetEmployeeId))) {
            throw new BusinessException(AttendanceErrorCode.NOT_DIRECT_LEADER);
        }
        return listRecords(targetEmployeeId, startDate, endDate);
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

    @Transactional
    public MakeupCompletionResponse completeMakeup(
            Long recordId,
            MakeupCompletionRequest request
    ) {
        AttendanceRecord record = attendanceMapper.findByIdForUpdate(recordId);
        if (record == null || !record.getEmployeeId().equals(request.employeeId())) {
            throw new BusinessException(AttendanceErrorCode.RECORD_NOT_FOUND);
        }

        AttendanceMakeupUsage existingUsage = attendanceMapper
                .findMakeupUsageByApplicationId(request.applicationId());
        LocalDate quotaMonth = YearMonth.from(record.getAttendanceDate()).atDay(1);
        if (existingUsage != null) {
            if (!existingUsage.getAttendanceRecordId().equals(recordId)
                    || !existingUsage.getEmployeeId().equals(request.employeeId())) {
                throw new BusinessException(AttendanceErrorCode.MAKEUP_APPLICATION_CONFLICT);
            }
            return completedMakeupResponse(request.applicationId(), record, quotaMonth);
        }

        if (!"LATE".equals(record.getAttendanceStatus())) {
            throw new BusinessException(AttendanceErrorCode.MAKEUP_REQUIRES_LATE_RECORD);
        }
        AttendanceMakeupQuota quota = attendanceMapper.findMakeupQuotaForUpdate(
                request.employeeId(), quotaMonth
        );
        if (quota == null || attendanceMapper.incrementMakeupQuotaUsedIfAvailable(
                request.employeeId(), quotaMonth
        ) == 0) {
            throw new BusinessException(AttendanceErrorCode.MAKEUP_QUOTA_UNAVAILABLE);
        }
        if (attendanceMapper.markRecordMadeUpIfLate(recordId, request.applicationId()) == 0) {
            throw new BusinessException(AttendanceErrorCode.MAKEUP_REQUIRES_LATE_RECORD);
        }

        AttendanceMakeupUsage usage = new AttendanceMakeupUsage();
        usage.setApplicationId(request.applicationId());
        usage.setAttendanceRecordId(recordId);
        usage.setEmployeeId(request.employeeId());
        usage.setQuotaMonth(quotaMonth);
        attendanceMapper.insertMakeupUsage(usage);
        return completedMakeupResponse(request.applicationId(), attendanceMapper.findById(recordId), quotaMonth);
    }

    private MakeupCompletionResponse completedMakeupResponse(
            Long applicationId,
            AttendanceRecord record,
            LocalDate quotaMonth
    ) {
        AttendanceMakeupQuota quota = attendanceMapper.findMakeupQuota(
                record.getEmployeeId(), quotaMonth
        );
        if (quota == null) {
            throw new BusinessException(AttendanceErrorCode.MAKEUP_QUOTA_UNAVAILABLE);
        }
        return MakeupCompletionResponse.from(applicationId, record, quota);
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
        return resolveClockStatus(now, clockProperties.getWorkStartTime());
    }

    private String resolveClockStatus(LocalDateTime now, LocalTime startTime) {
        if (!now.toLocalTime().isAfter(startTime)) {
            return NORMAL;
        }
        boolean absent = now.toLocalTime().isAfter(
                startTime.plusMinutes(
                        clockProperties.getLateThresholdMinutes()
                )
        );
        return absent ? ABSENT : LATE;
    }

    private String resolveDailyStatus(String morningStatus, String afternoonStatus) {
        if (ABSENT.equals(morningStatus) || ABSENT.equals(afternoonStatus)) {
            return ABSENT;
        }
        if (LATE.equals(morningStatus) || LATE.equals(afternoonStatus)) {
            return LATE;
        }
        return NORMAL.equals(morningStatus) ? afternoonStatus : morningStatus;
    }

    private void publishAttendanceAbnormal(
            Long recordId,
            Long employeeId,
            String eventSuffix,
            String abnormalLabel,
            String clockType,
            LocalDateTime occurredAt
    ) {
        notificationPublisher.publish(new PersonalNotificationEvent(
                "attendance:" + recordId + ":" + eventSuffix,
                employeeId,
                PersonalNotificationEvent.NotificationType.ATTENDANCE_ABNORMAL,
                "考勤异常提醒",
                "您于 " + occurredAt + " " + clockType + "打卡，系统判定为" + abnormalLabel,
                PersonalNotificationEvent.RelatedBusinessType.ATTENDANCE_RECORD,
                recordId,
                occurredAt
        ));
    }
}
