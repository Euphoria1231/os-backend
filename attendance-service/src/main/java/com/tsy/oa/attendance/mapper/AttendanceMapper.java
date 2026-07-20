package com.tsy.oa.attendance.mapper;

import com.tsy.oa.attendance.model.AttendanceRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AttendanceMapper {

    int insert(AttendanceRecord record);

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
}
