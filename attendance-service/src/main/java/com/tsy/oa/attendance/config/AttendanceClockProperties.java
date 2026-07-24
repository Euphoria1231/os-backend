package com.tsy.oa.attendance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalTime;

@Component
@RefreshScope
@ConfigurationProperties(prefix = "attendance.clock")
public class AttendanceClockProperties {

    private LocalTime workStartTime = LocalTime.of(9, 0);
    private LocalTime morningEndTime = LocalTime.of(12, 0);
    private LocalTime afternoonStartTime = LocalTime.of(14, 0);
    private LocalTime workEndTime = LocalTime.of(17, 0);
    private int lateThresholdMinutes = 30;
    private Duration lockTtl = Duration.ofSeconds(10);
    private Duration completedMarkerTtl = Duration.ofHours(36);

    public LocalTime getWorkStartTime() { return workStartTime; }
    public void setWorkStartTime(LocalTime workStartTime) { this.workStartTime = workStartTime; }
    public LocalTime getMorningEndTime() { return morningEndTime; }
    public void setMorningEndTime(LocalTime morningEndTime) { this.morningEndTime = morningEndTime; }
    public LocalTime getAfternoonStartTime() { return afternoonStartTime; }
    public void setAfternoonStartTime(LocalTime afternoonStartTime) { this.afternoonStartTime = afternoonStartTime; }
    public LocalTime getAfternoonClockStartTime() { return afternoonStartTime.minusMinutes(30); }
    public LocalTime getWorkEndTime() { return workEndTime; }
    public void setWorkEndTime(LocalTime workEndTime) { this.workEndTime = workEndTime; }
    public int getLateThresholdMinutes() { return lateThresholdMinutes; }
    public void setLateThresholdMinutes(int lateThresholdMinutes) { this.lateThresholdMinutes = lateThresholdMinutes; }
    public Duration getLockTtl() { return lockTtl; }
    public void setLockTtl(Duration lockTtl) { this.lockTtl = lockTtl; }
    public Duration getCompletedMarkerTtl() { return completedMarkerTtl; }
    public void setCompletedMarkerTtl(Duration completedMarkerTtl) { this.completedMarkerTtl = completedMarkerTtl; }
}
