package com.tsy.oa.notice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsy.oa.notice.message.event.ApprovalCompletedEvent;
import com.tsy.oa.notice.message.event.ApprovalCompletedEventConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = NoticeControllerTests.TestApplication.class)
@AutoConfigureMockMvc
class NoticeControllerTests {

    private static final String EMPLOYEE_HEADER = "X-Employee-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ApprovalCompletedEventConsumer approvalCompletedEventConsumer;

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM message");
        jdbcTemplate.update("DELETE FROM message_consume_record");
        jdbcTemplate.update("DELETE FROM notice_read");
        jdbcTemplate.update("DELETE FROM notice");
    }

    @Test
    void publishesNoticeAndTracksEmployeeReadStatus() throws Exception {
        long noticeId = publishNotice();

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
    void consumingSameApprovalEventCreatesOnlyOneMessage() throws Exception {
        ApprovalCompletedEvent event = approvalEvent("approval-completed:100:APPROVED", 100L, 10L, "APPROVED");

        approvalCompletedEventConsumer.consume(event);
        approvalCompletedEventConsumer.consume(event);

        Integer messageCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM message", Integer.class);
        Integer recordCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM message_consume_record WHERE event_id = ?",
                Integer.class,
                event.eventId()
        );
        assertEquals(1, messageCount);
        assertEquals(1, recordCount);

        mockMvc.perform(get("/api/notices/messages")
                        .header(EMPLOYEE_HEADER, "10")
                        .param("read", "false")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].businessId").value(100))
                .andExpect(jsonPath("$.data[0].businessType").value("APPROVAL"))
                .andExpect(jsonPath("$.data[0].read").value(false));
    }

    @Test
    void messageCanOnlyBeReadByRecipient() throws Exception {
        approvalCompletedEventConsumer.consume(approvalEvent("approval-completed:101:REJECTED", 101L, 10L, "REJECTED"));
        Long messageId = jdbcTemplate.queryForObject("SELECT id FROM message", Long.class);

        mockMvc.perform(get("/api/notices/messages/{id}", messageId).header(EMPLOYEE_HEADER, "20"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40402));

        mockMvc.perform(put("/api/notices/messages/{id}/read", messageId).header(EMPLOYEE_HEADER, "20"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40402));

        mockMvc.perform(get("/api/notices/messages/unread-count").header(EMPLOYEE_HEADER, "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));
        mockMvc.perform(put("/api/notices/messages/{id}/read", messageId).header(EMPLOYEE_HEADER, "10"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/notices/messages/{id}", messageId).header(EMPLOYEE_HEADER, "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(messageId))
                .andExpect(jsonPath("$.data.read").value(true))
                .andExpect(jsonPath("$.data.readAt").isNotEmpty());
        mockMvc.perform(get("/api/notices/messages/unread-count").header(EMPLOYEE_HEADER, "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0));
    }

    @Test
    void marksAllRecipientMessagesRead() throws Exception {
        approvalCompletedEventConsumer.consume(approvalEvent("approval-completed:102:APPROVED", 102L, 10L, "APPROVED"));
        approvalCompletedEventConsumer.consume(approvalEvent("approval-completed:103:REJECTED", 103L, 10L, "REJECTED"));
        approvalCompletedEventConsumer.consume(approvalEvent("approval-completed:104:APPROVED", 104L, 20L, "APPROVED"));

        mockMvc.perform(put("/api/notices/messages/read-all").header(EMPLOYEE_HEADER, "10"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/notices/messages/unread-count").header(EMPLOYEE_HEADER, "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0));
        mockMvc.perform(get("/api/notices/messages/unread-count").header(EMPLOYEE_HEADER, "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));
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
    void exposesOpenApiDocument() throws Exception {
        mockMvc.perform(get("/v3/api-docs")
                        .header("X-Forwarded-Host", "localhost")
                        .header("X-Forwarded-Port", "8088")
                        .header("X-Forwarded-Proto", "http"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").isNotEmpty())
                .andExpect(jsonPath("$.servers[0].url").value("http://localhost:8088"))
                .andExpect(jsonPath("$.paths['/api/notices']").exists())
                .andExpect(jsonPath("$.paths['/api/notices/messages']").exists());
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

    private ApprovalCompletedEvent approvalEvent(
            String eventId,
            Long applicationId,
            Long applicantId,
            String result
    ) {
        return new ApprovalCompletedEvent(
                eventId,
                applicationId,
                "LEAVE",
                applicantId,
                result,
                LocalDateTime.of(2026, 7, 22, 9, 0),
                "trace-" + applicationId
        );
    }

    private record NoticePayload(String title, String content) {
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan("com.tsy.oa.notice")
    static class TestApplication {
    }
}
