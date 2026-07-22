package com.tsy.oa.intelligence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = IntelligenceServiceApplicationTests.TestApplication.class)
@AutoConfigureMockMvc
class IntelligenceServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/intelligence/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value("intelligence-service"));
    }

    @Test
    void exposesOpenApiDocument() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("OA 搜索与智能平台 API"))
                .andExpect(jsonPath("$.servers[0].url").value("http://localhost:8088"))
                .andExpect(jsonPath("$.paths['/api/intelligence/health']").exists());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan("com.tsy.oa.intelligence")
    static class TestApplication {
    }
}
