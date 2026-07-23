package com.tsy.oa.user.employee;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PositionEmployeeControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearOrganizationData() {
        jdbcTemplate.update("DELETE FROM employee");
        jdbcTemplate.update("DELETE FROM position");
        jdbcTemplate.update("DELETE FROM department");
    }

    @Test
    void positionCrudFlowWorks() throws Exception {
        long positionId = createPosition("JAVA_DEV", "Java开发工程师");

        mockMvc.perform(get("/api/user/positions/{id}", positionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("JAVA_DEV"));

        mockMvc.perform(put("/api/user/positions/{id}", positionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(positionJson("JAVA_DEV", "高级Java开发工程师")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("高级Java开发工程师"));

        mockMvc.perform(get("/api/user/positions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(delete("/api/user/positions/{id}", positionId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/positions/{id}", positionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40402));
    }

    @Test
    void employeeCrudFlowHashesPasswordAndHidesItFromResponse() throws Exception {
        long departmentId = createDepartment("研发部");
        long positionId = createPosition("JAVA_DEV", "Java开发工程师");
        long employeeId = createEmployee("E001", "zhangsan", "Password123", departmentId, positionId);

        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM employee WHERE id = ?",
                String.class,
                employeeId
        );
        assertNotEquals("Password123", passwordHash);
        assertTrue(passwordHash.startsWith("$2"));

        mockMvc.perform(get("/api/user/employees/{id}", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("zhangsan"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        mockMvc.perform(put("/api/user/employees/{id}", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeUpdateJson("张三丰", departmentId, positionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.realName").value("张三丰"));

        mockMvc.perform(get("/api/user/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(delete("/api/user/employees/{id}", employeeId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/employees/{id}", employeeId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40403));
    }

    @Test
    void rejectsDuplicatePositionCodeAndEmployeeAccount() throws Exception {
        createPosition("JAVA_DEV", "Java开发工程师");
        mockMvc.perform(post("/api/user/positions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(positionJson("JAVA_DEV", "重复岗位")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40902));

        long departmentId = createDepartment("研发部");
        long positionId = jdbcTemplate.queryForObject(
                "SELECT id FROM position WHERE code = 'JAVA_DEV'",
                Long.class
        );
        createEmployee("E001", "zhangsan", "Password123", departmentId, positionId);

        mockMvc.perform(post("/api/user/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeCreateJson("E002", "zhangsan", "Password123", departmentId, positionId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40904));
    }

    @Test
    void returnsDirectAndDepartmentLeadersForApprovalRoute() throws Exception {
        long departmentId = createDepartment("研发部");
        long positionId = createPosition("JAVA_DEV", "Java开发工程师");
        long applicantId = createEmployee(
                "E001", "applicant", "Password123", departmentId, positionId
        );
        long directLeaderId = createEmployee(
                "E002", "directLeader", "Password123", departmentId, positionId
        );
        long departmentLeaderId = createEmployee(
                "E003", "departmentLeader", "Password123", departmentId, positionId
        );
        jdbcTemplate.update(
                "UPDATE employee SET leader_id = ? WHERE id = ?",
                directLeaderId,
                applicantId
        );
        jdbcTemplate.update(
                "UPDATE department SET leader_employee_id = ? WHERE id = ?",
                departmentLeaderId,
                departmentId
        );
        jdbcTemplate.update(
                "UPDATE employee SET real_name = '直属领导' WHERE id = ?",
                directLeaderId
        );
        jdbcTemplate.update(
                "UPDATE employee SET real_name = '部门负责人' WHERE id = ?",
                departmentLeaderId
        );

        mockMvc.perform(get("/internal/user/employees/{id}/approval-route", applicantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applicantId").value(applicantId))
                .andExpect(jsonPath("$.data.directLeaderId").value(directLeaderId))
                .andExpect(jsonPath("$.data.directLeaderName").value("直属领导"))
                .andExpect(jsonPath("$.data.departmentLeaderId").value(departmentLeaderId))
                .andExpect(jsonPath("$.data.departmentLeaderName").value("部门负责人"));
    }

    private long createDepartment(String name) throws Exception {
        String response = mockMvc.perform(post("/api/user/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DepartmentPayload(0L, name, null, 1, 1))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private long createPosition(String code, String name) throws Exception {
        String response = mockMvc.perform(post("/api/user/positions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(positionJson(code, name)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private long createEmployee(
            String employeeNo,
            String username,
            String password,
            long departmentId,
            long positionId
    ) throws Exception {
        String response = mockMvc.perform(post("/api/user/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeCreateJson(employeeNo, username, password, departmentId, positionId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode body = objectMapper.readTree(response);
        return body.path("data").path("id").asLong();
    }

    private String positionJson(String code, String name) throws Exception {
        return objectMapper.writeValueAsString(new PositionPayload(code, name, "研发岗位", 1));
    }

    private String employeeCreateJson(
            String employeeNo,
            String username,
            String password,
            long departmentId,
            long positionId
    ) throws Exception {
        return objectMapper.writeValueAsString(new EmployeeCreatePayload(
                employeeNo,
                username,
                password,
                "张三",
                departmentId,
                positionId,
                null,
                "13800000000",
                "zhangsan@example.com",
                1
        ));
    }

    private String employeeUpdateJson(String realName, long departmentId, long positionId) throws Exception {
        return objectMapper.writeValueAsString(new EmployeeUpdatePayload(
                realName,
                departmentId,
                positionId,
                null,
                "13900000000",
                "updated@example.com",
                1
        ));
    }

    private record DepartmentPayload(
            Long parentId,
            String name,
            Long leaderEmployeeId,
            Integer sortOrder,
            Integer status
    ) {
    }

    private record PositionPayload(String code, String name, String description, Integer status) {
    }

    private record EmployeeCreatePayload(
            String employeeNo,
            String username,
            String password,
            String realName,
            Long departmentId,
            Long positionId,
            Long leaderId,
            String phone,
            String email,
            Integer status
    ) {
    }

    private record EmployeeUpdatePayload(
            String realName,
            Long departmentId,
            Long positionId,
            Long leaderId,
            String phone,
            String email,
            Integer status
    ) {
    }
}
