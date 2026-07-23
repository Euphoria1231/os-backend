package com.tsy.oa.flow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsy.oa.common.exception.BusinessException;
import com.tsy.oa.flow.attendance.AttendanceMakeupGateway;
import com.tsy.oa.flow.employee.EmployeeDirectory;
import com.tsy.oa.flow.error.FlowErrorCode;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Autowired
    private InMemoryAttendanceMakeupGateway attendanceMakeupGateway;

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM approval_record");
        jdbcTemplate.update("DELETE FROM approval_task");
        jdbcTemplate.update("DELETE FROM flow_application");
        employeeDirectory.clear();
        employeeDirectory.setApprovalRoute(10L, 20L, 30L);
        attendanceMakeupGateway.clear();
        attendanceMakeupGateway.allow(
                1L, 10L, LocalDate.of(2026, 7, 20), 5
        );
    }

    @Test
    void activatesDepartmentLeaderAfterDirectLeaderApprovalAndCompletesOnSecondApproval() throws Exception {
        long applicationId = submit("/api/flow/applications/leave", 10L, "家庭事务");

        assertEquals(List.of("1:PENDING", "2:WAITING"), taskStates(applicationId));

        mockMvc.perform(get("/api/flow/tasks/todo").header(EMPLOYEE_HEADER, "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(applicationId))
                .andExpect(jsonPath("$.data[0].applicationType").value("LEAVE"));

        mockMvc.perform(post("/api/flow/tasks/{id}/approve", applicationId)
                .header(EMPLOYEE_HEADER, "20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"同意，请做好工作交接\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.approverId").value(30));

        assertEquals(List.of("1:APPROVED", "2:PENDING"), taskStates(applicationId));

        mockMvc.perform(get("/api/flow/tasks/todo").header(EMPLOYEE_HEADER, "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
        mockMvc.perform(get("/api/flow/tasks/done").header(EMPLOYEE_HEADER, "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].approvalLevel").value(1))
                .andExpect(jsonPath("$.data[0].action").value("APPROVE"));

        mockMvc.perform(get("/api/flow/tasks/todo").header(EMPLOYEE_HEADER, "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(applicationId));

        mockMvc.perform(post("/api/flow/tasks/{id}/approve", applicationId)
                        .header(EMPLOYEE_HEADER, "30")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"同意\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

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
    void submitsMakeupForLateRecordAndAssignsDirectLeader() throws Exception {
        mockMvc.perform(post("/api/flow/applications/makeup")
                        .header(EMPLOYEE_HEADER, "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"attendanceRecordId\":1,\"reason\":\"早高峰交通拥堵\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applicantId").value(10))
                .andExpect(jsonPath("$.data.approverId").value(20))
                .andExpect(jsonPath("$.data.applicationType").value("MAKEUP"))
                .andExpect(jsonPath("$.data.attendanceRecordId").value(1))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void rejectsDuplicateActiveMakeupApplication() throws Exception {
        submitMakeup(1L);

        mockMvc.perform(post("/api/flow/applications/makeup")
                        .header(EMPLOYEE_HEADER, "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"attendanceRecordId\":1,\"reason\":\"重复申请\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40902));
    }

    @Test
    void completesMakeupOnlyAfterDepartmentLeaderApproval() throws Exception {
        long applicationId = submitMakeup(1L);

        mockMvc.perform(post("/api/flow/tasks/{id}/approve", applicationId)
                        .header(EMPLOYEE_HEADER, "20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"同意补签\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        assertEquals(0, attendanceMakeupGateway.completionCount());

        mockMvc.perform(post("/api/flow/tasks/{id}/approve", applicationId)
                        .header(EMPLOYEE_HEADER, "30")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"同意补签\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        assertEquals(1, attendanceMakeupGateway.completionCount());
    }

    @Test
    void rejectsMakeupWithoutConsumingQuotaAndAllowsResubmission() throws Exception {
        long applicationId = submitMakeup(1L);

        mockMvc.perform(post("/api/flow/tasks/{id}/reject", applicationId)
                        .header(EMPLOYEE_HEADER, "20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"补签理由不足\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        submitMakeup(1L);
        assertEquals(0, attendanceMakeupGateway.completionCount());
    }

    @Test
    void keepsMakeupPendingWhenAttendanceCompletionFails() throws Exception {
        long applicationId = submitMakeup(1L);

        mockMvc.perform(post("/api/flow/tasks/{id}/approve", applicationId)
                        .header(EMPLOYEE_HEADER, "20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"直属领导同意\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        attendanceMakeupGateway.failCompletion();

        mockMvc.perform(post("/api/flow/tasks/{id}/approve", applicationId)
                        .header(EMPLOYEE_HEADER, "30")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"部门负责人同意\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40904));

        assertEquals("PENDING", jdbcTemplate.queryForObject(
                "SELECT status FROM flow_application WHERE id = ?",
                String.class,
                applicationId
        ));
        assertEquals(List.of("1:APPROVED", "2:PENDING"), taskStates(applicationId));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM approval_record WHERE application_id = ?",
                Integer.class,
                applicationId
        ));
    }

    @Test
    void createsOneTaskWhenBothApprovalLevelsUseSameEmployee() throws Exception {
        employeeDirectory.setApprovalRoute(10L, 20L, 20L);
        long applicationId = submit("/api/flow/applications/leave", 10L, "家庭事务");

        assertEquals(List.of("1:PENDING"), taskStates(applicationId));

        mockMvc.perform(post("/api/flow/tasks/{id}/approve", applicationId)
                        .header(EMPLOYEE_HEADER, "20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"同意\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.approvalProgress.length()").value(1));
    }

    @Test
    void rejectsSubmissionWithoutDepartmentLeader() throws Exception {
        employeeDirectory.setApprovalRoute(10L, 20L, null);

        mockMvc.perform(post("/api/flow/applications/leave")
                        .header(EMPLOYEE_HEADER, "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationJson("家庭事务")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(42202));
    }

    @Test
    void cancelsWaitingDepartmentTaskWhenDirectLeaderRejects() throws Exception {
        long applicationId = submit("/api/flow/applications/leave", 10L, "家庭事务");

        mockMvc.perform(post("/api/flow/tasks/{id}/reject", applicationId)
                        .header(EMPLOYEE_HEADER, "20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"不同意\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        assertEquals(List.of("1:REJECTED", "2:CANCELLED"), taskStates(applicationId));
    }

    @Test
    void rejectsRepeatedApprovalOfCompletedDirectLeaderTask() throws Exception {
        long applicationId = submit("/api/flow/applications/leave", 10L, "家庭事务");
        mockMvc.perform(post("/api/flow/tasks/{id}/approve", applicationId)
                        .header(EMPLOYEE_HEADER, "20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"同意\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/flow/tasks/{id}/approve", applicationId)
                        .header(EMPLOYEE_HEADER, "20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"重复同意\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40901));
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

    @Test
    void exposesApplicationsAsPagedSearchSource() throws Exception {
        long firstApplicationId = submit("/api/flow/applications/leave", 10L, "家庭事务");
        long secondApplicationId = submit("/api/flow/applications/overtime", 10L, "版本上线");

        mockMvc.perform(get("/internal/flow/search-source/{id}", firstApplicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(firstApplicationId))
                .andExpect(jsonPath("$.data.applicationType").value("LEAVE"))
                .andExpect(jsonPath("$.data.approverId").value(20));

        mockMvc.perform(get("/internal/flow/search-source")
                        .param("page", "1")
                        .param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(secondApplicationId))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(true));
    }

    @Test
    void rejectsSearchSourceOffsetBeyondMapperRange() throws Exception {
        mockMvc.perform(get("/internal/flow/search-source")
                        .param("page", String.valueOf(Integer.MAX_VALUE))
                        .param("pageSize", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
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

    private long submitMakeup(Long attendanceRecordId) throws Exception {
        String response = mockMvc.perform(post("/api/flow/applications/makeup")
                        .header(EMPLOYEE_HEADER, "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"attendanceRecordId\":" + attendanceRecordId
                                + ",\"reason\":\"早高峰交通拥堵\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
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

    private List<String> taskStates(long applicationId) {
        return jdbcTemplate.query(
                "SELECT approval_level, status FROM approval_task "
                        + "WHERE application_id = ? ORDER BY approval_level",
                (resultSet, rowNumber) -> resultSet.getInt("approval_level")
                        + ":" + resultSet.getString("status"),
                applicationId
        );
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

        @Bean
        @Primary
        InMemoryAttendanceMakeupGateway inMemoryAttendanceMakeupGateway() {
            return new InMemoryAttendanceMakeupGateway();
        }
    }

    static class InMemoryEmployeeDirectory implements EmployeeDirectory {

        private final Map<Long, Long> leaders = new ConcurrentHashMap<>();
        private final Map<Long, Long> departmentLeaders = new ConcurrentHashMap<>();

        @Override
        public ApprovalRoute findApprovalRoute(Long employeeId) {
            Long directLeaderId = leaders.get(employeeId);
            Long departmentLeaderId = departmentLeaders.get(employeeId);
            if (directLeaderId == null && departmentLeaderId == null) {
                return null;
            }
            return new ApprovalRoute(
                    employeeId,
                    directLeaderId,
                    directLeaderId == null ? null : "直属领导",
                    departmentLeaderId,
                    departmentLeaderId == null ? null : "部门负责人"
            );
        }

        void setApprovalRoute(Long employeeId, Long leaderId, Long departmentLeaderId) {
            if (leaderId != null) {
                leaders.put(employeeId, leaderId);
            } else {
                leaders.remove(employeeId);
            }
            if (departmentLeaderId != null) {
                departmentLeaders.put(employeeId, departmentLeaderId);
            } else {
                departmentLeaders.remove(employeeId);
            }
        }

        void clear() {
            leaders.clear();
            departmentLeaders.clear();
        }
    }

    static class InMemoryAttendanceMakeupGateway implements AttendanceMakeupGateway {

        private final Map<Long, MakeupEligibility> eligibilityByRecord = new ConcurrentHashMap<>();
        private final List<Long> completedApplications = new ArrayList<>();
        private boolean completionFails;

        @Override
        public MakeupEligibility getEligibility(Long attendanceRecordId, Long employeeId) {
            MakeupEligibility eligibility = eligibilityByRecord.get(attendanceRecordId);
            if (eligibility == null || !eligibility.employeeId().equals(employeeId)) {
                throw new BusinessException(FlowErrorCode.MAKEUP_NOT_ELIGIBLE);
            }
            return eligibility;
        }

        @Override
        public void completeMakeup(Long attendanceRecordId, Long employeeId, Long applicationId) {
            if (completionFails) {
                throw new BusinessException(FlowErrorCode.MAKEUP_COMPLETION_FAILED);
            }
            completedApplications.add(applicationId);
        }

        void allow(Long attendanceRecordId, Long employeeId, LocalDate date, int remainingCount) {
            eligibilityByRecord.put(
                    attendanceRecordId,
                    new MakeupEligibility(attendanceRecordId, employeeId, date, remainingCount)
            );
        }

        int completionCount() {
            return completedApplications.size();
        }

        void failCompletion() {
            completionFails = true;
        }

        void clear() {
            eligibilityByRecord.clear();
            completedApplications.clear();
            completionFails = false;
        }
    }
}
