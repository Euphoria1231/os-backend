package com.tsy.oa.attendance.report;

import com.alibaba.excel.annotation.ExcelProperty;
import com.tsy.oa.attendance.dto.AttendanceMonthlyStatisticsResponse;

import java.math.BigDecimal;

public class AttendanceMonthlyReportRow {

    @ExcelProperty(value = "员工ID", index = 0)
    private Long employeeId;

    @ExcelProperty(value = "月份", index = 1)
    private String month;

    @ExcelProperty(value = "应出勤天数", index = 2)
    private Integer expectedAttendanceDays;

    @ExcelProperty(value = "实际出勤天数", index = 3)
    private Integer actualAttendanceDays;

    @ExcelProperty(value = "迟到次数", index = 4)
    private Integer lateCount;

    @ExcelProperty(value = "早退次数", index = 5)
    private Integer earlyLeaveCount;

    @ExcelProperty(value = "旷工次数", index = 6)
    private Integer absenceCount;

    @ExcelProperty(value = "请假天数", index = 7)
    private Integer leaveDays;

    @ExcelProperty(value = "总工时", index = 8)
    private BigDecimal totalWorkHours;

    public static AttendanceMonthlyReportRow from(AttendanceMonthlyStatisticsResponse response) {
        AttendanceMonthlyReportRow row = new AttendanceMonthlyReportRow();
        row.employeeId = response.employeeId();
        row.month = response.month().toString();
        row.expectedAttendanceDays = response.expectedAttendanceDays();
        row.actualAttendanceDays = response.actualAttendanceDays();
        row.lateCount = response.lateCount();
        row.earlyLeaveCount = response.earlyLeaveCount();
        row.absenceCount = response.absenceCount();
        row.leaveDays = response.leaveDays();
        row.totalWorkHours = response.totalWorkHours();
        return row;
    }

    public Long getEmployeeId() { return employeeId; }
    public String getMonth() { return month; }
    public Integer getExpectedAttendanceDays() { return expectedAttendanceDays; }
    public Integer getActualAttendanceDays() { return actualAttendanceDays; }
    public Integer getLateCount() { return lateCount; }
    public Integer getEarlyLeaveCount() { return earlyLeaveCount; }
    public Integer getAbsenceCount() { return absenceCount; }
    public Integer getLeaveDays() { return leaveDays; }
    public BigDecimal getTotalWorkHours() { return totalWorkHours; }
}
