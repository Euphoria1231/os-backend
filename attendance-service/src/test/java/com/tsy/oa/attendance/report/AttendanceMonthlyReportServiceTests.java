package com.tsy.oa.attendance.report;

import com.tsy.oa.attendance.dto.AttendanceMonthlyStatisticsResponse;
import com.tsy.oa.attendance.service.AttendanceMonthlyStatisticsService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceMonthlyReportServiceTests {

    private static final YearMonth MONTH = YearMonth.of(2026, 7);

    @Mock
    private AttendanceMonthlyStatisticsService statisticsService;

    @InjectMocks
    private AttendanceMonthlyReportService reportService;

    @Test
    void exportsRequiredFieldsAndExpectedDataRows() throws Exception {
        when(statisticsService.findAuthorizedDepartmentEmployeeIds(2L, 2L))
                .thenReturn(List.of(10L, 11L));
        when(statisticsService.calculateMonthly(10L, MONTH)).thenReturn(monthlyResponse(
                10L, 23, 20, 2, 1, 1, 1, "160.00"
        ));
        when(statisticsService.calculateMonthly(11L, MONTH)).thenReturn(monthlyResponse(
                11L, 23, 21, 1, 0, 1, 0, "168.50"
        ));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        reportService.export(2L, MONTH, 2L, output);

        try (Workbook workbook = WorkbookFactory.create(
                new ByteArrayInputStream(output.toByteArray())
        )) {
            Sheet sheet = workbook.getSheet("月度考勤");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(3);
            Row header = sheet.getRow(0);
            assertThat(header.getPhysicalNumberOfCells()).isEqualTo(9);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("员工ID");
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("月份");
            assertThat(header.getCell(2).getStringCellValue()).isEqualTo("应出勤天数");
            assertThat(header.getCell(3).getStringCellValue()).isEqualTo("实际出勤天数");
            assertThat(header.getCell(4).getStringCellValue()).isEqualTo("迟到次数");
            assertThat(header.getCell(5).getStringCellValue()).isEqualTo("早退次数");
            assertThat(header.getCell(6).getStringCellValue()).isEqualTo("旷工次数");
            assertThat(header.getCell(7).getStringCellValue()).isEqualTo("请假天数");
            assertThat(header.getCell(8).getStringCellValue()).isEqualTo("总工时");
            Row firstData = sheet.getRow(1);
            assertThat(firstData.getCell(0).getNumericCellValue()).isEqualTo(10D);
            assertThat(firstData.getCell(1).getStringCellValue()).isEqualTo("2026-07");
            assertThat(firstData.getCell(8).getNumericCellValue()).isEqualTo(160D);
        }
    }

    @Test
    void exportsHeaderOnlyWhenDepartmentHasNoEmployees() throws Exception {
        when(statisticsService.findAuthorizedDepartmentEmployeeIds(1L, 9L)).thenReturn(List.of());
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        reportService.export(1L, MONTH, 9L, output);

        try (Workbook workbook = WorkbookFactory.create(
                new ByteArrayInputStream(output.toByteArray())
        )) {
            Sheet sheet = workbook.getSheet("月度考勤");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(1);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("员工ID");
        }
    }

    private AttendanceMonthlyStatisticsResponse monthlyResponse(
            Long employeeId,
            int expectedDays,
            int actualDays,
            int lateCount,
            int earlyLeaveCount,
            int absenceCount,
            int leaveDays,
            String totalWorkHours
    ) {
        return new AttendanceMonthlyStatisticsResponse(
                employeeId,
                MONTH,
                expectedDays,
                actualDays,
                lateCount,
                earlyLeaveCount,
                absenceCount,
                leaveDays,
                new BigDecimal(totalWorkHours)
        );
    }
}
