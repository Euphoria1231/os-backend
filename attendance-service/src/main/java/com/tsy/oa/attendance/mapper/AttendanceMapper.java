package com.tsy.oa.attendance.mapper;

import com.tsy.oa.attendance.model.AttendanceRecord;
import com.tsy.oa.attendance.model.AttendanceMakeupQuota;
import com.tsy.oa.attendance.model.AttendanceMakeupUsage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AttendanceMapper {

    int insert(AttendanceRecord record);

    AttendanceRecord findById(Long id);

    AttendanceRecord findByIdForUpdate(Long id);

    AttendanceRecord findByEmployeeAndDate(
            @Param("employeeId") Long employeeId,
            @Param("attendanceDate") LocalDate attendanceDate
    );

    int updateClockOut(
            @Param("id") Long id,
            @Param("clockOutTime") LocalDateTime clockOutTime
    );

    List<AttendanceRecord> findByEmployeeAndDateRange(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    int upsertMakeupQuota(AttendanceMakeupQuota quota);

    AttendanceMakeupQuota findMakeupQuota(
            @Param("employeeId") Long employeeId,
            @Param("quotaMonth") LocalDate quotaMonth
    );

    AttendanceMakeupQuota findMakeupQuotaForUpdate(
            @Param("employeeId") Long employeeId,
            @Param("quotaMonth") LocalDate quotaMonth
    );

    int incrementMakeupQuotaUsedIfAvailable(
            @Param("employeeId") Long employeeId,
            @Param("quotaMonth") LocalDate quotaMonth
    );

    int markRecordMadeUpIfLate(
            @Param("id") Long id,
            @Param("applicationId") Long applicationId
    );

    AttendanceMakeupUsage findMakeupUsageByApplicationId(Long applicationId);

    int insertMakeupUsage(AttendanceMakeupUsage usage);
}
