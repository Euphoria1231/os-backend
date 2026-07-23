package com.tsy.oa.intelligence.dashboard;

import com.tsy.oa.common.api.ApiResponse;
import com.tsy.oa.intelligence.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

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
                .standaloneSetup(new DashboardController(service))
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
}
