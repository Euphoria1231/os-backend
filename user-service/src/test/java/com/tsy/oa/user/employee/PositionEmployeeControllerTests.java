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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.nullValue;
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
        jdbcTemplate.update("DELETE FROM business_operation_log");
        jdbcTemplate.update("DELETE FROM employee_role");
        jdbcTemplate.update("DELETE FROM sys_role");
        jdbcTemplate.update("DELETE FROM employee");
        jdbcTemplate.update("DELETE FROM position");
        jdbcTemplate.update("DELETE FROM department");
    }

    @Test
    void positionCrudFlowWorks() throws Exception {
        long departmentId = createDepartment("研发部");
        long positionId = createPosition(departmentId, "JAVA_DEV", "Java开发工程师");

        mockMvc.perform(get("/api/user/positions/{id}", positionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("JAVA_DEV"))
                .andExpect(jsonPath("$.data.departmentId").value(departmentId))
                .andExpect(jsonPath("$.data.departmentName").value("研发部"));

        mockMvc.perform(put("/api/user/positions/{id}", positionId)
                        .header("X-Employee-Id", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(positionJson(departmentId, "JAVA_DEV", "高级Java开发工程师")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("高级Java开发工程师"));

        mockMvc.perform(get("/api/user/positions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(delete("/api/user/positions/{id}", positionId)
                        .header("X-Employee-Id", 99L))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/positions/{id}", positionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40402));

        assertEquals(1, logCount("CREATE_POSITION", "SUCCESS"));
        assertEquals(1, logCount("UPDATE_POSITION", "SUCCESS"));
        assertEquals(1, logCount("DELETE_POSITION", "SUCCESS"));
    }

    @Test
    void employeeCrudFlowHashesPasswordAndHidesItFromResponse() throws Exception {
        long departmentId = createDepartment("研发部");
        long positionId = createPosition(departmentId, "JAVA_DEV", "Java开发工程师");
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
                        .header("X-Employee-Id", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeUpdateJson("张三丰", departmentId, positionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.realName").value("张三丰"));

        mockMvc.perform(get("/api/user/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(delete("/api/user/employees/{id}", employeeId)
                        .header("X-Employee-Id", 99L))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/employees/{id}", employeeId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40403));

        assertEquals(1, logCount("CREATE_EMPLOYEE", "SUCCESS"));
        assertEquals(1, logCount("UPDATE_EMPLOYEE", "SUCCESS"));
        assertEquals(1, logCount("DELETE_EMPLOYEE", "SUCCESS"));
    }

    @Test
    void rejectsDuplicatePositionCodeAndEmployeeAccount() throws Exception {
        long departmentId = createDepartment("研发部");
        createPosition(departmentId, "JAVA_DEV", "Java开发工程师");
        mockMvc.perform(post("/api/user/positions")
                        .header("X-Employee-Id", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(positionJson(departmentId, "JAVA_DEV", "重复岗位")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40902));

        long positionId = jdbcTemplate.queryForObject(
                "SELECT id FROM position WHERE code = 'JAVA_DEV'",
                Long.class
        );
        createEmployee("E001", "zhangsan", "Password123", departmentId, positionId);

        mockMvc.perform(post("/api/user/employees")
                        .header("X-Employee-Id", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeCreateJson(
                                "E002", "zhangsan", "Password123", departmentId, positionId, null
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40904));

        assertEquals(1, logCount("CREATE_POSITION", "FAILURE"));
        assertEquals(1, logCount("CREATE_EMPLOYEE", "FAILURE"));
    }

    @Test
    void returnsDirectAndDepartmentLeadersForApprovalRoute() throws Exception {
        long departmentId = createDepartment("研发部");
        long positionId = createPosition(departmentId, "JAVA_DEV", "Java开发工程师");
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

    @Test
    void rejectsEmployeeWhenPositionBelongsToAnotherDepartment() throws Exception {
        long researchDepartmentId = createDepartment("研发部");
        long administrationDepartmentId = createDepartment("综合管理部");
        long positionId = createPosition(researchDepartmentId, "JAVA_DEV", "Java开发工程师");

        mockMvc.perform(post("/api/user/employees")
                        .header("X-Employee-Id", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeCreateJson(
                                "E001", "employee", "Password123",
                                administrationDepartmentId, positionId, null
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40908));
    }

    @Test
    void rejectsEmployeeWhenDirectLeaderBelongsToAnotherDepartment() throws Exception {
        long researchDepartmentId = createDepartment("研发部");
        long administrationDepartmentId = createDepartment("综合管理部");
        long researchPositionId = createPosition(researchDepartmentId, "JAVA_DEV", "Java开发工程师");
        long administrationPositionId = createPosition(
                administrationDepartmentId, "ADMIN_STAFF", "行政专员"
        );
        long leaderId = createEmployee(
                "E001", "leader", "Password123", researchDepartmentId, researchPositionId
        );

        mockMvc.perform(post("/api/user/employees")
                        .header("X-Employee-Id", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeCreateJson(
                                "E002", "employee", "Password123",
                                administrationDepartmentId, administrationPositionId, leaderId
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40909));
    }

    @Test
    void rejectsDepartmentLeaderOutsideDepartment() throws Exception {
        long researchDepartmentId = createDepartment("研发部");
        long administrationDepartmentId = createDepartment("综合管理部");
        long researchPositionId = createPosition(researchDepartmentId, "JAVA_DEV", "Java开发工程师");
        long leaderId = createEmployee(
                "E001", "leader", "Password123", researchDepartmentId, researchPositionId
        );

        mockMvc.perform(put("/api/user/departments/{id}", administrationDepartmentId)
                        .header("X-Employee-Id", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(departmentJson("综合管理部", leaderId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40910));
    }

    @Test
    void rejectsAssigningExistingEmployeeAsLeaderWhileCreatingDepartment() throws Exception {
        long researchDepartmentId = createDepartment("研发部");
        long researchPositionId = createPosition(researchDepartmentId, "JAVA_DEV", "Java开发工程师");
        long leaderId = createEmployee(
                "E001", "leader", "Password123", researchDepartmentId, researchPositionId
        );

        mockMvc.perform(post("/api/user/departments")
                        .header("X-Employee-Id", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(departmentJson("产品部", leaderId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40910));
    }

    @Test
    void rejectsSuperAdminAsDepartmentLeader() throws Exception {
        long departmentId = createDepartment("综合管理部");
        long positionId = createPosition(departmentId, "SYSTEM_ADMIN", "系统管理员");
        long adminId = createEmployee(
                "E001", "admin", "Password123", departmentId, positionId
        );
        jdbcTemplate.update(
                "INSERT INTO sys_role (id, code, name, status) VALUES (1, 'SUPER_ADMIN', '超级管理员', 1)"
        );
        jdbcTemplate.update(
                "INSERT INTO employee_role (employee_id, role_id) VALUES (?, 1)",
                adminId
        );

        mockMvc.perform(put("/api/user/departments/{id}", departmentId)
                        .header("X-Employee-Id", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(departmentJson("综合管理部", adminId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40911));
    }

    @Test
    void excludesInvalidLegacyLeadersFromApprovalRoute() throws Exception {
        long applicantDepartmentId = createDepartment("综合管理部");
        long leaderDepartmentId = createDepartment("研发部");
        long applicantPositionId = createPosition(applicantDepartmentId, "ADMIN_STAFF", "行政专员");
        long leaderPositionId = createPosition(leaderDepartmentId, "JAVA_DEV", "Java开发工程师");
        long applicantId = createEmployee(
                "E001", "applicant", "Password123", applicantDepartmentId, applicantPositionId
        );
        long outsideLeaderId = createEmployee(
                "E002", "leader", "Password123", leaderDepartmentId, leaderPositionId
        );
        jdbcTemplate.update("UPDATE employee SET leader_id = ? WHERE id = ?", outsideLeaderId, applicantId);
        jdbcTemplate.update(
                "UPDATE department SET leader_employee_id = ? WHERE id = ?",
                outsideLeaderId,
                applicantDepartmentId
        );

        mockMvc.perform(get("/internal/user/employees/{id}/approval-route", applicantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.directLeaderId").value(nullValue()))
                .andExpect(jsonPath("$.data.departmentLeaderId").value(nullValue()));
    }

    @Test
    void rejectsMovingAssignedPositionToAnotherDepartment() throws Exception {
        long researchDepartmentId = createDepartment("研发部");
        long administrationDepartmentId = createDepartment("综合管理部");
        long positionId = createPosition(researchDepartmentId, "JAVA_DEV", "Java开发工程师");
        createEmployee("E001", "employee", "Password123", researchDepartmentId, positionId);

        mockMvc.perform(put("/api/user/positions/{id}", positionId)
                        .header("X-Employee-Id", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(positionJson(administrationDepartmentId, "JAVA_DEV", "Java开发工程师")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40912));
    }

    @Test
    void rejectsMovingLeaderWhileDirectReportsStillReferenceThem() throws Exception {
        long researchDepartmentId = createDepartment("研发部");
        long administrationDepartmentId = createDepartment("综合管理部");
        long researchPositionId = createPosition(researchDepartmentId, "JAVA_DEV", "Java开发工程师");
        long administrationPositionId = createPosition(
                administrationDepartmentId, "ADMIN_STAFF", "行政专员"
        );
        long leaderId = createEmployee(
                "E001", "leader", "Password123", researchDepartmentId, researchPositionId
        );
        createEmployee(
                "E002", "employee", "Password123",
                researchDepartmentId, researchPositionId, leaderId
        );

        mockMvc.perform(put("/api/user/employees/{id}", leaderId)
                        .header("X-Employee-Id", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeUpdateJson(
                                "直属领导", administrationDepartmentId, administrationPositionId
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40913));
    }

    @Test
    void rejectsMovingDepartmentLeaderWhileDepartmentStillReferencesThem() throws Exception {
        long researchDepartmentId = createDepartment("研发部");
        long administrationDepartmentId = createDepartment("综合管理部");
        long researchPositionId = createPosition(researchDepartmentId, "JAVA_DEV", "Java开发工程师");
        long administrationPositionId = createPosition(
                administrationDepartmentId, "ADMIN_STAFF", "行政专员"
        );
        long leaderId = createEmployee(
                "E001", "leader", "Password123", researchDepartmentId, researchPositionId
        );
        jdbcTemplate.update(
                "UPDATE department SET leader_employee_id = ? WHERE id = ?",
                leaderId,
                researchDepartmentId
        );

        mockMvc.perform(put("/api/user/employees/{id}", leaderId)
                        .header("X-Employee-Id", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeUpdateJson(
                                "部门负责人", administrationDepartmentId, administrationPositionId
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40913));
    }

    private long createDepartment(String name) throws Exception {
        String response = mockMvc.perform(post("/api/user/departments")
                        .header("X-Employee-Id", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DepartmentPayload(0L, name, null, 1, 1))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private long createPosition(long departmentId, String code, String name) throws Exception {
        String response = mockMvc.perform(post("/api/user/positions")
                        .header("X-Employee-Id", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(positionJson(departmentId, code, name)))
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
                        .header("X-Employee-Id", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeCreateJson(
                                employeeNo, username, password, departmentId, positionId, null
                        )))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode body = objectMapper.readTree(response);
        return body.path("data").path("id").asLong();
    }

    private long createEmployee(
            String employeeNo,
            String username,
            String password,
            long departmentId,
            long positionId,
            Long leaderId
    ) throws Exception {
        String response = mockMvc.perform(post("/api/user/employees")
                        .header("X-Employee-Id", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeCreateJson(
                                employeeNo, username, password, departmentId, positionId, leaderId
                        )))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private int logCount(String operationType, String status) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM business_operation_log WHERE operation_type = ? AND operation_status = ?",
                Integer.class,
                operationType,
                status
        );
    }

    private String positionJson(long departmentId, String code, String name) throws Exception {
        return objectMapper.writeValueAsString(new PositionPayload(
                departmentId, code, name, "研发岗位", 1
        ));
    }

    private String employeeCreateJson(
            String employeeNo,
            String username,
            String password,
            long departmentId,
            long positionId,
            Long leaderId
    ) throws Exception {
        return objectMapper.writeValueAsString(new EmployeeCreatePayload(
                employeeNo,
                username,
                password,
                "张三",
                departmentId,
                positionId,
                leaderId,
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

    private String departmentJson(String name, Long leaderEmployeeId) throws Exception {
        return objectMapper.writeValueAsString(new DepartmentPayload(
                0L, name, leaderEmployeeId, 1, 1
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

    private record PositionPayload(
            Long departmentId,
            String code,
            String name,
            String description,
            Integer status
    ) {
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
