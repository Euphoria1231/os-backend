package com.tsy.oa.flow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsy.oa.flow.employee.EmployeeDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = FlowControllerTests.TestApplication.class)
@AutoConfigureMockMvc
@Import(FlowControllerTests.FlowTestConfiguration.class)
class FlowControllerTests {

    private static final String EMPLOYEE_HEADER = "X-Employee-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InMemoryEmployeeDirectory employeeDirectory;

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM approval_record");
        jdbcTemplate.update("DELETE FROM flow_application");
        employeeDirectory.clear();
        employeeDirectory.setLeader(10L, 20L);
    }

    @Test
    void submitsLeaveAndCompletesLeaderApproval() throws Exception {
        long applicationId = submit("/api/flow/applications/leave", 10L, "家庭事务");

        mockMvc.perform(get("/api/flow/tasks/todo").header(EMPLOYEE_HEADER, "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(applicationId))
                .andExpect(jsonPath("$.data[0].applicationType").value("LEAVE"));

        mockMvc.perform(post("/api/flow/tasks/{id}/approve", applicationId)
                        .header(EMPLOYEE_HEADER, "20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"同意，请做好工作交接\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(get("/api/flow/tasks/todo").header(EMPLOYEE_HEADER, "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
        mockMvc.perform(get("/api/flow/tasks/done").header(EMPLOYEE_HEADER, "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].action").value("APPROVE"));
        mockMvc.perform(get("/api/flow/applications/mine").header(EMPLOYEE_HEADER, "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("APPROVED"));
    }

    @Test
    void rejectsApprovalByNonLeader() throws Exception {
        long applicationId = submit("/api/flow/applications/leave", 10L, "家庭事务");

        mockMvc.perform(post("/api/flow/tasks/{id}/approve", applicationId)
                        .header(EMPLOYEE_HEADER, "30")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"越权审批\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    @Test
    void submitsAndRejectsOvertimeApplication() throws Exception {
        long applicationId = submit("/api/flow/applications/overtime", 10L, "版本上线");

        mockMvc.perform(post("/api/flow/tasks/{id}/reject", applicationId)
                        .header(EMPLOYEE_HEADER, "20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"时间安排不合理\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    void rejectsSubmissionWithoutDirectLeader() throws Exception {
        employeeDirectory.clear();

        mockMvc.perform(post("/api/flow/applications/leave")
                        .header(EMPLOYEE_HEADER, "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationJson("家庭事务")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(42201));
    }

    @Test
    void exposesOpenApiDocument() throws Exception {
        mockMvc.perform(get("/v3/api-docs")
                        .header("X-Forwarded-Host", "localhost")
                        .header("X-Forwarded-Port", "8088")
                        .header("X-Forwarded-Proto", "http"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").isNotEmpty())
                .andExpect(jsonPath("$.servers[0].url").value("http://localhost:8088"))
                .andExpect(jsonPath("$.paths['/api/flow/applications/leave']").exists());
    }

    private long submit(String path, Long applicantId, String reason) throws Exception {
        String response = mockMvc.perform(post(path)
                        .header(EMPLOYEE_HEADER, applicantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationJson(reason)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.approverId").value(20))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode body = objectMapper.readTree(response);
        return body.path("data").path("id").asLong();
    }

    private String applicationJson(String reason) throws Exception {
        return objectMapper.writeValueAsString(new ApplicationPayload(
                LocalDateTime.of(2026, 7, 21, 9, 0),
                LocalDateTime.of(2026, 7, 21, 18, 0),
                reason
        ));
    }

    private record ApplicationPayload(LocalDateTime startTime, LocalDateTime endTime, String reason) {
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan("com.tsy.oa.flow")
    static class TestApplication {
    }

    @TestConfiguration
    static class FlowTestConfiguration {

        @Bean
        @Primary
        InMemoryEmployeeDirectory inMemoryEmployeeDirectory() {
            return new InMemoryEmployeeDirectory();
        }
    }

    static class InMemoryEmployeeDirectory implements EmployeeDirectory {

        private final Map<Long, Long> leaders = new ConcurrentHashMap<>();

        @Override
        public Long findDirectLeaderId(Long employeeId) {
            return leaders.get(employeeId);
        }

        void setLeader(Long employeeId, Long leaderId) {
            leaders.put(employeeId, leaderId);
        }

        void clear() {
            leaders.clear();
        }
    }
}
