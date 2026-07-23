package com.tsy.oa.intelligence.ai.analysis;

import com.tsy.oa.intelligence.IntelligenceServiceApplication;
import com.tsy.oa.intelligence.ai.AiProvider;
import com.tsy.oa.intelligence.ai.model.AiCallResult;
import com.tsy.oa.intelligence.ai.model.AiCallStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = IntelligenceServiceApplication.class)
@AutoConfigureMockMvc
@Import(AiAnalysisControllerWebTests.TestBeans.class)
class AiAnalysisControllerWebTests {

    @Autowired private MockMvc mockMvc;

    @Test
    void mapsClientInputFailuresToBadRequestBusinessCode() throws Exception {
        mockMvc.perform(post("/api/intelligence/ai/office/ask").header("X-Employee-Id", "1")
                        .contentType("application/json").content("{bad json"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(40000));
        mockMvc.perform(post("/api/intelligence/ai/office/ask").header("X-Employee-Id", "1")
                        .contentType("application/json").content("{\"question\":\" \"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(40000));
        mockMvc.perform(post("/api/intelligence/ai/office/ask").header("X-Employee-Id", "1")
                        .contentType("application/json").content("{\"question\":\"" + "a".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(40000));
        mockMvc.perform(post("/api/intelligence/ai/attendance/1/analysis").header("X-Employee-Id", "1"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(40000));
        mockMvc.perform(post("/api/intelligence/ai/approvals/0/analysis").header("X-Employee-Id", "1"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(40000));
        mockMvc.perform(get("/api/intelligence/ai/analyses/0").header("X-Employee-Id", "1"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(40000));
        mockMvc.perform(post("/api/intelligence/ai/office/ask").header("X-Employee-Id", "0")
                        .contentType("application/json").content("{\"question\":\"制度\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void acceptsAValidOfficeQuestion() throws Exception {
        mockMvc.perform(post("/api/intelligence/ai/office/ask").header("X-Employee-Id", "1")
                        .contentType("application/json").content("{\"question\":\"如何补打卡？\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.disclaimer").value("仅供参考"));
    }

    @TestConfiguration
    static class TestBeans {
        @Bean @Primary ApplicationAnalysisSource applicationAnalysisSource() {
            return id -> new com.tsy.oa.intelligence.search.event.source.ApplicationSearchSourceClient.ApplicationSearchSourceResponse(
                    id, 1L, 2L, "LEAVE", "PENDING", "reason", LocalDateTime.now(), LocalDateTime.now()); }
        @Bean @Primary AttendanceAnalysisSource attendanceAnalysisSource() { return (id, start, end) -> List.of(); }
        @Bean @Primary AiProvider testAiProvider() { return prompt -> new AiCallResult(AiCallStatus.SUCCESS, "safe advice"); }
    }
}
