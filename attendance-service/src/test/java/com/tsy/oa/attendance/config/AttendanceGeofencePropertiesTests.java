package com.tsy.oa.attendance.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AttendanceGeofencePropertiesTests {

    @Test
    void defaultsClockRadiusToOneKilometer() {
        AttendanceGeofenceProperties properties = new AttendanceGeofenceProperties();

        assertEquals(1000, properties.getRadiusMeters());
    }
}
