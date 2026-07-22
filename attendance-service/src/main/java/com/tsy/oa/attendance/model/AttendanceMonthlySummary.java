package com.tsy.oa.attendance.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class AttendanceMonthlySummary {

    private Long id;
    private Long employeeId;
    private LocalDate summaryMonth;
    private Integer expectedAttendanceDays;
    private Integer actualAttendanceDays;
    private Integer lateCount;
    private Integer earlyLeaveCount;
    private Integer absenceCount;
    private Integer leaveDays;
    private BigDecimal totalWorkHours;
    private LocalDateTime calculatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public LocalDate getSummaryMonth() { return summaryMonth; }
    public void setSummaryMonth(LocalDate summaryMonth) { this.summaryMonth = summaryMonth; }
    public Integer getExpectedAttendanceDays() { return expectedAttendanceDays; }
    public void setExpectedAttendanceDays(Integer expectedAttendanceDays) { this.expectedAttendanceDays = expectedAttendanceDays; }
    public Integer getActualAttendanceDays() { return actualAttendanceDays; }
    public void setActualAttendanceDays(Integer actualAttendanceDays) { this.actualAttendanceDays = actualAttendanceDays; }
    public Integer getLateCount() { return lateCount; }
    public void setLateCount(Integer lateCount) { this.lateCount = lateCount; }
    public Integer getEarlyLeaveCount() { return earlyLeaveCount; }
    public void setEarlyLeaveCount(Integer earlyLeaveCount) { this.earlyLeaveCount = earlyLeaveCount; }
    public Integer getAbsenceCount() { return absenceCount; }
    public void setAbsenceCount(Integer absenceCount) { this.absenceCount = absenceCount; }
    public Integer getLeaveDays() { return leaveDays; }
    public void setLeaveDays(Integer leaveDays) { this.leaveDays = leaveDays; }
    public BigDecimal getTotalWorkHours() { return totalWorkHours; }
    public void setTotalWorkHours(BigDecimal totalWorkHours) { this.totalWorkHours = totalWorkHours; }
    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
