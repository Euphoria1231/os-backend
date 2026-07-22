package com.tsy.oa.attendance.controller;

import com.tsy.oa.attendance.AttendanceServiceApplication;
import com.tsy.oa.attendance.report.AttendanceMonthlyReportService;
import com.tsy.oa.attendance.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.OutputStream;
import java.time.YearMonth;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AttendanceReportController.class)
@ContextConfiguration(classes = AttendanceServiceApplication.class)
@Import(GlobalExceptionHandler.class)
class AttendanceReportControllerTests {

    private static final String EMPLOYEE_HEADER = "X-Employee-Id";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AttendanceMonthlyReportService reportService;

    @Test
    void downloadsMonthlyReportWithExcelHeadersAndUtf8FileName() throws Exception {
        byte[] workbook = new byte[]{1, 2, 3};
        doAnswer(invocation -> {
            invocation.getArgument(3, OutputStream.class).write(workbook);
            return null;
        }).when(reportService).export(
                eq(2L), eq(YearMonth.of(2026, 7)), eq(2L), any(OutputStream.class)
        );

        mockMvc.perform(get("/api/attendance/reports/monthly/export")
                        .header(EMPLOYEE_HEADER, "2")
                        .param("month", "2026-07")
                        .param("departmentId", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .andExpect(header().string(
                        "Content-Disposition",
                        containsString("attachment; filename*=UTF-8''%E8%80%83%E5%8B%A4")
                ))
                .andExpect(content().bytes(workbook));
    }

    @Test
    void rejectsInvalidReportMonth() throws Exception {
        mockMvc.perform(get("/api/attendance/reports/monthly/export")
                        .header(EMPLOYEE_HEADER, "2")
                        .param("month", "2026-13")
                        .param("departmentId", "2"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reportService);
    }
}
