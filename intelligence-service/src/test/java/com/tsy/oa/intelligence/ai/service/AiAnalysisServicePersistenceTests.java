package com.tsy.oa.intelligence.ai.service;

import com.tsy.oa.intelligence.IntelligenceServiceApplication;
import com.tsy.oa.intelligence.ai.AiProvider;
import com.tsy.oa.intelligence.ai.model.AiCallResult;
import com.tsy.oa.intelligence.ai.model.AiCallStatus;
import com.tsy.oa.intelligence.ai.model.AiPrompt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = IntelligenceServiceApplication.class)
@Import(AiAnalysisServicePersistenceTests.TestBeans.class)
class AiAnalysisServicePersistenceTests {

    @Autowired
    private AiAnalysisService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM ai_analysis_record");
    }

    @Test
    void recordsOnlyAuditableMetadataAndReferenceOnlySummary() {
        String sensitivePrompt = "Do not persist this sensitive prompt";
        var result = service.analyze(new AiAnalysisRequest(
                "ATTENDANCE",
                "attendance-42",
                sensitivePrompt
        ));

        assertThat(result.status()).isEqualTo(AiCallStatus.SUCCESS);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT request_type FROM ai_analysis_record WHERE business_reference_id = ?",
                String.class,
                "attendance-42"
        )).isEqualTo("ATTENDANCE");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM ai_analysis_record WHERE business_reference_id = ?",
                String.class,
                "attendance-42"
        )).isEqualTo("SUCCESS");
        String resultSummary = jdbcTemplate.queryForObject(
                "SELECT result_summary FROM ai_analysis_record WHERE business_reference_id = ?",
                String.class,
                "attendance-42"
        );
        assertThat(resultSummary).contains("仅供参考").doesNotContain(sensitivePrompt);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT business_reference_id FROM ai_analysis_record WHERE request_type = ?",
                String.class,
                "ATTENDANCE"
        )).isEqualTo("attendance-42");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT duration_ms FROM ai_analysis_record WHERE business_reference_id = ?",
                Long.class,
                "attendance-42"
        )).isGreaterThanOrEqualTo(0L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT audited_at FROM ai_analysis_record WHERE business_reference_id = ?",
                java.time.LocalDateTime.class,
                "attendance-42"
        )).isNotNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'ai_analysis_record' "
                        + "AND column_name IN ('prompt', 'api_key', 'request_body')",
                Integer.class
        )).isZero();
    }

    @TestConfiguration
    static class TestBeans {

        @Bean
        @Primary
        AiProvider testAiProvider() {
            return (AiPrompt prompt) -> new AiCallResult(
                    AiCallStatus.SUCCESS,
                    prompt.content()
            );
        }
    }
}
