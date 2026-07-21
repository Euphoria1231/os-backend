package com.tsy.oa.notice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @BeforeEach
    void resetData() {
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

    private record NoticePayload(String title, String content) {
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan("com.tsy.oa.notice")
    static class TestApplication {
    }
}
