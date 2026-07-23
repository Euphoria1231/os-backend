package com.tsy.oa.intelligence.dashboard;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.intelligence.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardControllerWebTests {

    @Test
    void returnsOrganizationSectionToSuperAdministrator() throws Exception {
        OrganizationDashboardClient client = () -> ApiResponse.success(
                new OrganizationDashboardClient.OrganizationStatisticsResponse(
                        3,
                        2,
                        1,
                        List.of(new NameCountResponse("研发部", 2)),
                        List.of(new NameCountResponse("开发工程师", 2))
                )
        );
        OrganizationDashboardService service = new OrganizationDashboardService(client);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new DashboardController(
                        service,
                        unusedAttendanceService(),
                        unusedApprovalService()
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/intelligence/dashboard/organization")
                        .header("X-Roles", "SUPER_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.data.totalEmployees").value(3))
                .andExpect(jsonPath("$.data.data.departmentEmployeeCounts[0].name").value("研发部"));

        mockMvc.perform(get("/api/intelligence/dashboard/organization"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    void returnsAttendanceSectionForRequestedMonth() throws Exception {
        AttendanceDashboardClient client = month -> ApiResponse.success(
                new AttendanceDashboardClient.AttendanceStatisticsResponse(
                        month,
                        10,
                        2,
                        1,
                        0,
                        List.of(new AttendanceDailyTrendResponse(
                                LocalDate.of(2026, 7, 1), 12, 10, 2, 0, 0
                        ))
                )
        );
        AttendanceDashboardService service = new AttendanceDashboardService(client);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new DashboardController(
                        unusedOrganizationService(),
                        service,
                        unusedApprovalService()
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/intelligence/dashboard/attendance")
                        .header("X-Roles", "SUPER_ADMIN")
                        .param("month", "2026-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.data.month").value("2026-07"))
                .andExpect(jsonPath("$.data.data.normalCount").value(10))
                .andExpect(jsonPath("$.data.data.dailyTrend[0].totalCount").value(12));

        mockMvc.perform(get("/api/intelligence/dashboard/attendance")
                        .header("X-Roles", "SUPER_ADMIN")
                        .param("month", "2026-13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void returnsApprovalSectionForRequestedMonth() throws Exception {
        ApprovalDashboardClient client = month -> ApiResponse.success(
                new ApprovalDashboardClient.ApprovalStatisticsResponse(
                        month,
                        3,
                        5,
                        1,
                        List.of(new ApprovalTypeDistributionResponse("LEAVE", 6)),
                        List.of(new ApprovalDailyTrendResponse(LocalDate.of(2026, 7, 1), 9))
                )
        );
        ApprovalDashboardService service = new ApprovalDashboardService(client);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new DashboardController(
                        unusedOrganizationService(),
                        unusedAttendanceService(),
                        service
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/intelligence/dashboard/approvals")
                        .header("X-Roles", "SUPER_ADMIN")
                        .param("month", "2026-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.data.pendingCount").value(3))
                .andExpect(jsonPath("$.data.data.approvedCount").value(5))
                .andExpect(jsonPath("$.data.data.typeDistribution[0].applicationType").value("LEAVE"))
                .andExpect(jsonPath("$.data.data.dailyTrend[0].applicationCount").value(9));
    }

    private OrganizationDashboardService unusedOrganizationService() {
        return new OrganizationDashboardService(() -> ApiResponse.failure(50000, "unused"));
    }

    private AttendanceDashboardService unusedAttendanceService() {
        return new AttendanceDashboardService(month -> ApiResponse.failure(50000, "unused"));
    }

    private ApprovalDashboardService unusedApprovalService() {
        return new ApprovalDashboardService(month -> ApiResponse.failure(50000, "unused"));
    }
}
