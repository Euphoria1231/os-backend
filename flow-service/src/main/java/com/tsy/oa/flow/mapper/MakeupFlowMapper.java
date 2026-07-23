package com.tsy.oa.flow.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MakeupFlowMapper {

    Long findActiveApplicationIdByAttendanceRecordId(Long attendanceRecordId);

    int releaseActiveMarker(@Param("applicationId") Long applicationId);
}
