package com.tsy.oa.user.log;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OperationLogControllerTests {

    private static final String EMPLOYEE_HEADER = "X-Employee-Id";
    private static final String ROLES_HEADER = "X-Roles";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM business_operation_log");
        jdbcTemplate.update("DELETE FROM employee");
        jdbcTemplate.update("DELETE FROM position");
        jdbcTemplate.update("DELETE FROM department");
        jdbcTemplate.update("INSERT INTO department (id, name) VALUES (1, '研发部')");
        jdbcTemplate.update("INSERT INTO position (id, code, name) VALUES (1, 'DEV', '开发工程师')");
        jdbcTemplate.update(
                "INSERT INTO employee "
                        + "(id, employee_no, username, password_hash, real_name, department_id, position_id) "
                        + "VALUES (10, 'E010', 'zhangsan', 'hash', '张三', 1, 1), "
                        + "(20, 'E020', 'lisi', 'hash', '李四', 1, 1)"
        );
    }

    @Test
    void storesResolvedOperatorNameAndReturnsOnlyCurrentEmployeesLogs() throws Exception {
        appendLog(10L, "伪造姓名", "FLOW", "SUBMIT", "SUCCESS", "提交请假申请");
        appendLog(20L, "伪造姓名", "NOTICE", "PUBLISH", "SUCCESS", "发布公告");

        mockMvc.perform(get("/api/user/operation-logs/mine")
                        .header(EMPLOYEE_HEADER, "10")
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].operatorId").value(10))
                .andExpect(jsonPath("$.data.items[0].operatorName").value("张三"))
                .andExpect(jsonPath("$.data.items[0].businessModule").value("FLOW"));
    }

    @Test
    void allowsOnlySuperAdminToQueryAllLogsWithFilters() throws Exception {
        appendLog(10L, "张三", "FLOW", "SUBMIT", "SUCCESS", "提交请假申请");
        appendLog(20L, "李四", "NOTICE", "PUBLISH", "FAILURE", "发布公告失败");

        mockMvc.perform(get("/api/user/operation-logs")
                        .header(EMPLOYEE_HEADER, "10")
                        .header(ROLES_HEADER, "EMPLOYEE"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        mockMvc.perform(get("/api/user/operation-logs")
                        .header(EMPLOYEE_HEADER, "1")
                        .header(ROLES_HEADER, "SUPER_ADMIN")
                        .param("operatorKeyword", "李四")
                        .param("businessModule", "NOTICE")
                        .param("operationStatus", "FAILURE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].operatorId").value(20))
                .andExpect(jsonPath("$.data.items[0].operationStatus").value("FAILURE"));
    }

    @Test
    void redactsSecretsAndAbsolutePathsBeforePersistence() throws Exception {
        mockMvc.perform(post("/internal/user/operation-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operatorId": 10,
                                  "operatorName": "伪造姓名",
                                  "serviceName": "intelligence-service",
                                  "businessModule": "AI",
                                  "operationType": "ANALYZE",
                                  "targetType": "AI_ANALYSIS",
                                  "targetId": "99",
                                  "summary": "password=secret Authorization=Bearer abc.def DASHSCOPE_API_KEY=sk-123",
                                  "operationStatus": "FAILURE",
                                  "requestPath": "/api/intelligence/ai/approvals/99/analysis",
                                  "httpMethod": "POST",
                                  "clientIp": "127.0.0.1",
                                  "errorMessage": "C:\\\\Users\\\\TangSY\\\\project\\\\file.java database_password=root"
                                }
                                """))
                .andExpect(status().isOk());

        String summary = jdbcTemplate.queryForObject(
                "SELECT summary FROM business_operation_log", String.class
        );
        String errorMessage = jdbcTemplate.queryForObject(
                "SELECT error_message FROM business_operation_log", String.class
        );
        assertTrue(summary.contains("[REDACTED]"));
        assertFalse(summary.contains("secret"));
        assertFalse(summary.contains("abc.def"));
        assertFalse(summary.contains("sk-123"));
        assertTrue(errorMessage.contains("[PATH]"));
        assertFalse(errorMessage.contains("TangSY"));
        assertFalse(errorMessage.contains("root"));
    }

    private void appendLog(
            Long operatorId,
            String operatorName,
            String businessModule,
            String operationType,
            String operationStatus,
            String summary
    ) throws Exception {
        mockMvc.perform(post("/internal/user/operation-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operatorId": %d,
                                  "operatorName": "%s",
                                  "serviceName": "user-service",
                                  "businessModule": "%s",
                                  "operationType": "%s",
                                  "targetType": "APPLICATION",
                                  "targetId": "1",
                                  "summary": "%s",
                                  "operationStatus": "%s",
                                  "requestPath": "/api/test",
                                  "httpMethod": "POST",
                                  "clientIp": "127.0.0.1"
                                }
                                """.formatted(
                                operatorId,
                                operatorName,
                                businessModule,
                                operationType,
                                summary,
                                operationStatus
                        )))
                .andExpect(status().isOk());
    }
}
