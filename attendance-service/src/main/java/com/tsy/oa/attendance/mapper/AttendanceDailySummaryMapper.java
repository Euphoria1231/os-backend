package com.tsy.oa.attendance.mapper;

import com.tsy.oa.attendance.model.AttendanceDailySummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AttendanceDailySummaryMapper {

    int insert(AttendanceDailySummary summary);

    int upsertForRecalculation(AttendanceDailySummary summary);

    AttendanceDailySummary findByEmployeeAndWorkDate(
            @Param("employeeId") Long employeeId,
            @Param("workDate") LocalDate workDate
    );

    List<AttendanceDailySummary> findByEmployeeAndDateRange(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
