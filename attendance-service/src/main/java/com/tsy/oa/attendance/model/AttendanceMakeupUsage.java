package com.tsy.oa.attendance.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AttendanceMakeupUsage {

    private Long id;
    private Long applicationId;
    private Long attendanceRecordId;
    private Long employeeId;
    private LocalDate quotaMonth;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public Long getAttendanceRecordId() { return attendanceRecordId; }
    public void setAttendanceRecordId(Long attendanceRecordId) { this.attendanceRecordId = attendanceRecordId; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public LocalDate getQuotaMonth() { return quotaMonth; }
    public void setQuotaMonth(LocalDate quotaMonth) { this.quotaMonth = quotaMonth; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
