package com.tsy.oa.attendance.mapper;

import com.tsy.oa.attendance.model.AttendanceMonthlySummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface AttendanceMonthlySummaryMapper {

    int upsert(AttendanceMonthlySummary summary);

    AttendanceMonthlySummary findByEmployeeAndMonth(
            @Param("employeeId") Long employeeId,
            @Param("summaryMonth") LocalDate summaryMonth
    );
}
