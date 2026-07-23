package com.tsy.oa.notice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsy.oa.common.log.BusinessOperationLogger;
import com.tsy.oa.common.log.OperationLogCommand;
import com.tsy.oa.common.notification.PersonalNotificationEvent;
import com.tsy.oa.notice.event.PersonalNotificationEventHandler;
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

import java.util.List;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = NoticeControllerTests.TestApplication.class)
@AutoConfigureMockMvc
@Import(NoticeControllerTests.NoticeTestConfiguration.class)
class NoticeControllerTests {

    private static final String EMPLOYEE_HEADER = "X-Employee-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RecordedOperationLogs operationLogs;

    @Autowired
    private PersonalNotificationEventHandler notificationEventHandler;

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM personal_notification");
        jdbcTemplate.update("DELETE FROM notice_read");
        jdbcTemplate.update("DELETE FROM notice");
        operationLogs.clear();
    }

    @Test
    void listsOnlyCurrentEmployeesPersonalNotificationsAndKeepsUnreadCountsSeparate() throws Exception {
        insertPersonalNotification("event-20", 20L, "新的审批待办", "申请 L202607240001 等待处理");
        insertPersonalNotification("event-30", 30L, "申请已批准", "申请 L202607240002 已通过");
        publishNotice();

        mockMvc.perform(get("/api/notices/personal")
                        .header(EMPLOYEE_HEADER, "20")
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].eventId").value("event-20"))
                .andExpect(jsonPath("$.data.items[0].title").value("新的审批待办"))
                .andExpect(jsonPath("$.data.items[0].read").value(false))
                .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(get("/api/notices/personal/unread-count")
                        .header(EMPLOYEE_HEADER, "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));
        mockMvc.perform(get("/api/notices/unread-count")
                        .header(EMPLOYEE_HEADER, "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    void marksPersonalNotificationsReadWithoutCrossEmployeeAccess() throws Exception {
        long firstId = insertPersonalNotification(
                "event-20-1", 20L, "新的审批待办", "申请 L202607240001 等待处理"
        );
        insertPersonalNotification(
                "event-20-2", 20L, "申请已批准", "申请 L202607240002 已通过"
        );
        long anotherEmployeesId = insertPersonalNotification(
                "event-30-1", 30L, "考勤异常提醒", "今日上班打卡判定为迟到"
        );

        mockMvc.perform(put("/api/notices/personal/{id}/read", firstId)
                        .header(EMPLOYEE_HEADER, "20"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/notices/personal/{id}/read", anotherEmployeesId)
                        .header(EMPLOYEE_HEADER, "20"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40402));
        mockMvc.perform(put("/api/notices/personal/read-all")
                        .header(EMPLOYEE_HEADER, "20"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/notices/personal/unread-count")
                        .header(EMPLOYEE_HEADER, "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM personal_notification "
                        + "WHERE recipient_employee_id = ? AND read_at IS NULL",
                Integer.class,
                30L
        ));
    }

    @Test
    void consumesEachPersonalNotificationEventOnlyOnce() throws Exception {
        PersonalNotificationEvent event = new PersonalNotificationEvent(
                "flow-submit-1001",
                20L,
                PersonalNotificationEvent.NotificationType.APPROVAL_TASK,
                "新的审批待办",
                "申请 L202607240001 等待处理",
                PersonalNotificationEvent.RelatedBusinessType.FLOW_APPLICATION,
                1001L,
                LocalDateTime.of(2026, 7, 24, 9, 30)
        );

        assertTrue(notificationEventHandler.handle(event));
        assertFalse(notificationEventHandler.handle(event));

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM personal_notification WHERE event_id = ?",
                Integer.class,
                event.eventId()
        ));
        mockMvc.perform(get("/api/notices/personal")
                        .header(EMPLOYEE_HEADER, "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].eventId").value(event.eventId()))
                .andExpect(jsonPath("$.data.items[0].createdAt").value("2026-07-24T09:30:00"));
    }

    @Test
    void rejectsInvalidPersonalNotificationPagination() throws Exception {
        mockMvc.perform(get("/api/notices/personal")
                        .header(EMPLOYEE_HEADER, "20")
                        .param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
        mockMvc.perform(get("/api/notices/personal")
                        .header(EMPLOYEE_HEADER, "20")
                        .param("pageSize", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void publishesNoticeAndTracksEmployeeReadStatus() throws Exception {
        long noticeId = publishNotice();

        assertEquals(1, operationLogs.count("PUBLISH_NOTICE", "SUCCESS"));

        mockMvc.perform(get("/api/notices").header(EMPLOYEE_HEADER, "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(noticeId))
                .andExpect(jsonPath("$.data[0].read").value(false));
        mockMvc.perform(get("/api/notices/unread-count").header(EMPLOYEE_HEADER, "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));

        mockMvc.perform(put("/api/notices/{id}/read", noticeId).header(EMPLOYEE_HEADER, "20"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/notices/{id}", noticeId).header(EMPLOYEE_HEADER, "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("关于国庆节放假的通知"))
                .andExpect(jsonPath("$.data.read").value(true))
                .andExpect(jsonPath("$.data.readAt").isNotEmpty());
        mockMvc.perform(get("/api/notices/unread-count").header(EMPLOYEE_HEADER, "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0));
    }

    @Test
    void markingNoticeReadIsIdempotent() throws Exception {
        long noticeId = publishNotice();

        mockMvc.perform(put("/api/notices/{id}/read", noticeId).header(EMPLOYEE_HEADER, "20"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/notices/{id}/read", noticeId).header(EMPLOYEE_HEADER, "20"))
                .andExpect(status().isOk());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notice_read WHERE notice_id = ? AND employee_id = ?",
                Integer.class,
                noticeId,
                20L
        );
        assertEquals(1, count);
    }

    @Test
    void validatesPublishRequestAndMissingNotice() throws Exception {
        mockMvc.perform(post("/api/notices")
                        .header(EMPLOYEE_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"content\":\"公告正文\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));

        mockMvc.perform(put("/api/notices/{id}/read", 999L).header(EMPLOYEE_HEADER, "20"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void updatesAndSoftDeletesPublishedNotice() throws Exception {
        long noticeId = publishNotice();

        mockMvc.perform(put("/api/notices/{id}", noticeId)
                        .header(EMPLOYEE_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NoticePayload(
                                "国庆节放假安排（更新）",
                                "公司国庆节放假安排已经更新。"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("国庆节放假安排（更新）"));

        mockMvc.perform(delete("/api/notices/{id}", noticeId)
                        .header(EMPLOYEE_HEADER, "1"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/notices/{id}", noticeId)
                        .header(EMPLOYEE_HEADER, "20"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
        assertEquals("DELETED", jdbcTemplate.queryForObject(
                "SELECT status FROM notice WHERE id = ?",
                String.class,
                noticeId
        ));
        assertEquals(1, operationLogs.count("UPDATE_NOTICE", "SUCCESS"));
        assertEquals(1, operationLogs.count("DELETE_NOTICE", "SUCCESS"));
    }

    @Test
    void recordsFailedUpdateAndDeleteForMissingNotice() throws Exception {
        mockMvc.perform(put("/api/notices/{id}", 999L)
                        .header(EMPLOYEE_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NoticePayload(
                                "公告标题",
                                "公告正文"
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));

        mockMvc.perform(delete("/api/notices/{id}", 999L)
                        .header(EMPLOYEE_HEADER, "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));

        assertEquals(1, operationLogs.count("UPDATE_NOTICE", "FAILURE"));
        assertEquals(1, operationLogs.count("DELETE_NOTICE", "FAILURE"));
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
                .andExpect(jsonPath("$.paths['/api/notices']").exists());
    }

    @Test
    void exposesPublishedNoticesAsPagedSearchSource() throws Exception {
        long firstNoticeId = publishNotice();
        long secondNoticeId = publishNotice();

        mockMvc.perform(get("/internal/notices/search-source/{id}", firstNoticeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(firstNoticeId))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        mockMvc.perform(get("/internal/notices/search-source")
                        .param("page", "1")
                        .param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(secondNoticeId))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(true));
    }

    @Test
    void rejectsSearchSourceOffsetBeyondMapperRange() throws Exception {
        mockMvc.perform(get("/internal/notices/search-source")
                        .param("page", String.valueOf(Integer.MAX_VALUE))
                        .param("pageSize", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    private long publishNotice() throws Exception {
        String response = mockMvc.perform(post("/api/notices")
                        .header(EMPLOYEE_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NoticePayload(
                                "关于国庆节放假的通知",
                                "公司国庆节放假安排如下。"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publisherId").value(1))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode body = objectMapper.readTree(response);
        return body.path("data").path("id").asLong();
    }

    private long insertPersonalNotification(
            String eventId,
            Long recipientEmployeeId,
            String title,
            String content
    ) {
        jdbcTemplate.update(
                "INSERT INTO personal_notification "
                        + "(recipient_employee_id, notification_type, title, content, "
                        + "related_business_type, related_business_id, event_id, created_at) "
                        + "VALUES (?, 'APPROVAL_TASK', ?, ?, 'FLOW_APPLICATION', 1001, ?, CURRENT_TIMESTAMP)",
                recipientEmployeeId,
                title,
                content,
                eventId
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM personal_notification WHERE event_id = ?",
                Long.class,
                eventId
        );
    }

    private record NoticePayload(String title, String content) {
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan("com.tsy.oa.notice")
    static class TestApplication {
    }

    @TestConfiguration
    static class NoticeTestConfiguration {

        @Bean
        RecordedOperationLogs recordedOperationLogs() {
            return new RecordedOperationLogs();
        }

        @Bean
        @Primary
        BusinessOperationLogger testBusinessOperationLogger(RecordedOperationLogs logs) {
            return new BusinessOperationLogger(
                    "notice-service", logs::add, (command, exception) -> {
                    }
            );
        }
    }

    static class RecordedOperationLogs {

        private final List<OperationLogCommand> commands = new java.util.concurrent.CopyOnWriteArrayList<>();

        void add(OperationLogCommand command) {
            commands.add(command);
        }

        long count(String operationType, String status) {
            return commands.stream()
                    .filter(command -> operationType.equals(command.operationType()))
                    .filter(command -> status.equals(command.operationStatus()))
                    .count();
        }

        void clear() {
            commands.clear();
        }
    }
}
