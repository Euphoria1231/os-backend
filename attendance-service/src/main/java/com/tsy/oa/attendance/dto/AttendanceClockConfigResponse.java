package com.tsy.oa.attendance.dto;

import com.tsy.oa.attendance.config.AttendanceClockProperties;
import com.tsy.oa.attendance.config.AttendanceGeofenceProperties;

public record AttendanceClockConfigResponse(
        String morningStartTime,
        String morningEndTime,
        String afternoonStartTime,
        String afternoonEndTime,
        double centerLongitude,
        double centerLatitude,
        int radiusMeters
) {

    public static AttendanceClockConfigResponse from(
            AttendanceClockProperties clockProperties,
            AttendanceGeofenceProperties geofenceProperties
    ) {
        return new AttendanceClockConfigResponse(
                clockProperties.getWorkStartTime().toString(),
                clockProperties.getMorningEndTime().toString(),
                clockProperties.getAfternoonStartTime().toString(),
                clockProperties.getWorkEndTime().toString(),
                geofenceProperties.getCenterLongitude(),
                geofenceProperties.getCenterLatitude(),
                geofenceProperties.getRadiusMeters()
        );
    }
}
