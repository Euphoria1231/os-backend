package com.tsy.oa.attendance.controller;

import com.tsy.oa.attendance.report.AttendanceMonthlyReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/attendance/reports")
public class AttendanceReportController {

    private static final String EMPLOYEE_HEADER = "X-Employee-Id";
    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final AttendanceMonthlyReportService reportService;

    public AttendanceReportController(AttendanceMonthlyReportService reportService) {
        this.reportService = reportService;
    }

    @Operation(summary = "导出部门月度考勤报表")
    @GetMapping("/monthly/export")
    public void exportMonthlyReport(
            @Parameter(hidden = true) @RequestHeader(EMPLOYEE_HEADER) Long operatorEmployeeId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam Long departmentId,
            HttpServletResponse response
    ) throws IOException {
        String fileName = "考勤月报-" + month + "-部门" + departmentId + ".xlsx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setContentType(XLSX_CONTENT_TYPE);
        response.setHeader(
                "Content-Disposition",
                "attachment; filename*=UTF-8''" + encodedFileName
        );
        reportService.export(operatorEmployeeId, month, departmentId, response.getOutputStream());
    }
}
