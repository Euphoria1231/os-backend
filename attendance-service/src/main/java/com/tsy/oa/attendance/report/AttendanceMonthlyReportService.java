package com.tsy.oa.attendance.report;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.tsy.oa.attendance.service.AttendanceMonthlyStatisticsService;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.time.YearMonth;
import java.util.List;

@Service
public class AttendanceMonthlyReportService {

    private static final int WRITE_BATCH_SIZE = 200;

    private final AttendanceMonthlyStatisticsService statisticsService;

    public AttendanceMonthlyReportService(AttendanceMonthlyStatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    public void export(
            Long operatorEmployeeId,
            YearMonth month,
            Long departmentId,
            OutputStream outputStream
    ) {
        List<Long> employeeIds = statisticsService.findAuthorizedDepartmentEmployeeIds(
                operatorEmployeeId,
                departmentId
        );
        ExcelWriter writer = EasyExcel.write(outputStream, AttendanceMonthlyReportRow.class)
                .autoCloseStream(false)
                .build();
        try {
            WriteSheet sheet = EasyExcel.writerSheet("月度考勤").build();
            if (employeeIds.isEmpty()) {
                writer.write(List.of(), sheet);
                return;
            }
            for (int start = 0; start < employeeIds.size(); start += WRITE_BATCH_SIZE) {
                int end = Math.min(start + WRITE_BATCH_SIZE, employeeIds.size());
                List<AttendanceMonthlyReportRow> rows = employeeIds.subList(start, end).stream()
                        .map(employeeId -> statisticsService.calculateMonthly(employeeId, month))
                        .map(AttendanceMonthlyReportRow::from)
                        .toList();
                writer.write(rows, sheet);
            }
        } finally {
            writer.finish();
        }
    }
}
