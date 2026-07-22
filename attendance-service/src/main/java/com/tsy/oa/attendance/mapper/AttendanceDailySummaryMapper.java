package com.tsy.oa.attendance.mapper;

import com.tsy.oa.attendance.model.AttendanceDailySummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface AttendanceDailySummaryMapper {

    int insert(AttendanceDailySummary summary);

    int upsertForRecalculation(AttendanceDailySummary summary);

    AttendanceDailySummary findByEmployeeAndWorkDate(
            @Param("employeeId") Long employeeId,
            @Param("workDate") LocalDate workDate
    );
}
