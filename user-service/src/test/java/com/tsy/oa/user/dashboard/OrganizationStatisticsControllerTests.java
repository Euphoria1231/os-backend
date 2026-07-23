package com.tsy.oa.user.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrganizationStatisticsControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpOrganization() {
        jdbcTemplate.update("DELETE FROM employee_role");
        jdbcTemplate.update("DELETE FROM employee");
        jdbcTemplate.update("DELETE FROM position");
        jdbcTemplate.update("DELETE FROM department");

        jdbcTemplate.update("INSERT INTO department (id, name, status) VALUES (1, '研发部', 1)");
        jdbcTemplate.update("INSERT INTO department (id, name, status) VALUES (2, '行政部', 1)");
        jdbcTemplate.update("INSERT INTO position (id, code, name, status) VALUES (1, 'DEV', '开发工程师', 1)");
        jdbcTemplate.update("INSERT INTO position (id, code, name, status) VALUES (2, 'HR', '行政专员', 1)");
        insertEmployee(1, "E001", "alice", "Alice", 1, 1, 1);
        insertEmployee(2, "E002", "bob", "Bob", 1, 1, 1);
        insertEmployee(3, "E003", "carol", "Carol", 2, 2, 0);
    }

    @Test
    void returnsOrganizationStatisticsForDashboard() throws Exception {
        mockMvc.perform(get("/internal/user/dashboard/organization"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalEmployees").value(3))
                .andExpect(jsonPath("$.data.enabledEmployees").value(2))
                .andExpect(jsonPath("$.data.disabledEmployees").value(1))
                .andExpect(jsonPath("$.data.departmentEmployeeCounts[0].name").value("研发部"))
                .andExpect(jsonPath("$.data.departmentEmployeeCounts[0].count").value(2))
                .andExpect(jsonPath("$.data.positionEmployeeCounts[0].name").value("开发工程师"))
                .andExpect(jsonPath("$.data.positionEmployeeCounts[0].count").value(2));
    }

    private void insertEmployee(
            long id,
            String employeeNo,
            String username,
            String realName,
            long departmentId,
            long positionId,
            int status
    ) {
        jdbcTemplate.update(
                "INSERT INTO employee "
                        + "(id, employee_no, username, password_hash, real_name, department_id, position_id, status) "
                        + "VALUES (?, ?, ?, 'hash', ?, ?, ?, ?)",
                id,
                employeeNo,
                username,
                realName,
                departmentId,
                positionId,
                status
        );
    }
}
