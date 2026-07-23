package com.tsy.oa.attendance.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AttendanceRecord {

    private Long id;
    private Long employeeId;
    private LocalDate attendanceDate;
    private LocalDateTime clockInTime;
    private LocalDateTime clockOutTime;
    private String attendanceStatus;
    private String originalAttendanceStatus;
    private Long makeupApplicationId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }
    public LocalDateTime getClockInTime() { return clockInTime; }
    public void setClockInTime(LocalDateTime clockInTime) { this.clockInTime = clockInTime; }
    public LocalDateTime getClockOutTime() { return clockOutTime; }
    public void setClockOutTime(LocalDateTime clockOutTime) { this.clockOutTime = clockOutTime; }
    public String getAttendanceStatus() { return attendanceStatus; }
    public void setAttendanceStatus(String attendanceStatus) { this.attendanceStatus = attendanceStatus; }
    public String getOriginalAttendanceStatus() { return originalAttendanceStatus; }
    public void setOriginalAttendanceStatus(String originalAttendanceStatus) {
        this.originalAttendanceStatus = originalAttendanceStatus;
    }
    public Long getMakeupApplicationId() { return makeupApplicationId; }
    public void setMakeupApplicationId(Long makeupApplicationId) {
        this.makeupApplicationId = makeupApplicationId;
    }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
