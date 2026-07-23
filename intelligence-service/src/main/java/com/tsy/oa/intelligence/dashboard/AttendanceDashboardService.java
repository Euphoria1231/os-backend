package com.tsy.oa.intelligence.dashboard;

import com.tsy.oa.common.api.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AttendanceDashboardService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceDashboardService.class);
    private static final String FAILURE_MESSAGE = "考勤统计暂时不可用";

    private final AttendanceDashboardClient client;

    public AttendanceDashboardService(AttendanceDashboardClient client) {
        this.client = client;
    }

    public DashboardSectionResponse<AttendanceDashboardResponse> getAttendance(String month) {
        String normalizedMonth = DashboardMonth.normalize(month);
        try {
            ApiResponse<AttendanceDashboardClient.AttendanceStatisticsResponse> response =
                    client.statistics(normalizedMonth);
            if (response == null || response.code() != 0 || response.data() == null) {
                throw new IllegalStateException("attendance-service returned an unsuccessful response");
            }
            AttendanceDashboardClient.AttendanceStatisticsResponse data = response.data();
            return DashboardSectionResponse.success(new AttendanceDashboardResponse(
                    data.month(),
                    data.normalCount(),
                    data.lateCount(),
                    data.earlyLeaveCount(),
                    data.absentCount(),
                    data.dailyTrend()
            ));
        } catch (RuntimeException exception) {
            log.warn("Attendance dashboard source is unavailable: {}", exception.getClass().getSimpleName());
            return DashboardSectionResponse.failed(FAILURE_MESSAGE);
        }
    }
}
