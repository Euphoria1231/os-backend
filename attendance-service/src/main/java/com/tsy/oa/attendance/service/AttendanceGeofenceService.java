package com.tsy.oa.attendance.service;

import com.tsy.oa.attendance.config.AttendanceGeofenceProperties;
import com.tsy.oa.attendance.dto.ClockLocationRequest;
import com.tsy.oa.attendance.error.AttendanceErrorCode;
import com.tsy.oa.common.exception.BusinessException;
import org.springframework.stereotype.Service;

@Service
public class AttendanceGeofenceService {

    private static final double EARTH_RADIUS_METERS = 6_371_008.8;

    private final AttendanceGeofenceProperties properties;

    public AttendanceGeofenceService(AttendanceGeofenceProperties properties) {
        this.properties = properties;
    }

    public void requireInside(ClockLocationRequest request) {
        if (distanceMeters(request.longitude(), request.latitude())
                > properties.getRadiusMeters()) {
            throw new BusinessException(AttendanceErrorCode.OUTSIDE_CLOCK_AREA);
        }
    }

    double distanceMeters(double longitude, double latitude) {
        double centerLatitudeRadians = Math.toRadians(properties.getCenterLatitude());
        double latitudeRadians = Math.toRadians(latitude);
        double latitudeDelta = latitudeRadians - centerLatitudeRadians;
        double longitudeDelta = Math.toRadians(longitude - properties.getCenterLongitude());
        double haversine = Math.pow(Math.sin(latitudeDelta / 2), 2)
                + Math.cos(centerLatitudeRadians) * Math.cos(latitudeRadians)
                * Math.pow(Math.sin(longitudeDelta / 2), 2);
        double normalizedHaversine = Math.min(1, Math.max(0, haversine));
        double angularDistance = 2 * Math.atan2(
                Math.sqrt(normalizedHaversine),
                Math.sqrt(1 - normalizedHaversine)
        );
        return EARTH_RADIUS_METERS * angularDistance;
    }
}
