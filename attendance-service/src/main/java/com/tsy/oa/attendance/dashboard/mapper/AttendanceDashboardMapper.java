package com.tsy.oa.attendance.dashboard.mapper;

import com.tsy.oa.attendance.dashboard.dto.AttendanceDailyTrendResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AttendanceDashboardMapper {

    List<AttendanceDailyTrendResponse> findDailyTrend(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
