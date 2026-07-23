package com.tsy.oa.intelligence.ai.service;

import com.tsy.oa.intelligence.IntelligenceServiceApplication;
import com.tsy.oa.intelligence.ai.AiProvider;
import com.tsy.oa.intelligence.ai.model.AiCallResult;
import com.tsy.oa.intelligence.ai.model.AiCallStatus;
import com.tsy.oa.intelligence.ai.model.AiPrompt;
import com.tsy.oa.intelligence.ai.persistence.AiAnalysisRecord;
import com.tsy.oa.intelligence.ai.persistence.AiAnalysisRecordMapper;
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

    @Autowired
    private AiAnalysisRecordMapper recordMapper;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM ai_analysis_record");
    }

    @Test
    void recordsOnlyAuditableMetadataAndReferenceOnlySummary() {
        String sensitivePrompt = "Do not persist this sensitive prompt SECRET_PROMPT";
        var result = service.analyzeAndRecord(new AiAnalysisRequest(
                "ATTENDANCE",
                "attendance-42",
                42L,
                sensitivePrompt
        ));

        assertThat(result.result().status()).isEqualTo(AiCallStatus.SUCCESS);
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
        assertThat(resultSummary).contains("仅供参考").doesNotContain(sensitivePrompt, "SECRET_DISPLAY");
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

    @Test
    void insertsAndHydratesCompleteAuditRecordThroughRealMyBatisMapper() {
        AiAnalysisRecord inserted = new AiAnalysisRecord("APPROVAL", "99", 7L, "FAILED", 12L,
                "仅供参考：APPROVAL AI analysis failed.", java.time.LocalDateTime.of(2026, 7, 23, 9, 0));

        recordMapper.insert(inserted);
        AiAnalysisRecord found = recordMapper.findById(inserted.getId());

        assertThat(inserted.getId()).isPositive();
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(inserted.getId());
        assertThat(found.getRequestType()).isEqualTo("APPROVAL");
        assertThat(found.getBusinessReferenceId()).isEqualTo("99");
        assertThat(found.getInitiatorEmployeeId()).isEqualTo(7L);
        assertThat(found.getStatus()).isEqualTo("FAILED");
        assertThat(found.getDurationMs()).isEqualTo(12L);
        assertThat(found.getResultSummary()).isEqualTo("仅供参考：APPROVAL AI analysis failed.");
        assertThat(found.getAuditedAt()).isEqualTo(java.time.LocalDateTime.of(2026, 7, 23, 9, 0));
    }

    @Test
    void hydratesLegacyRecordWithNullInitiatorThroughRealMyBatisMapper() {
        jdbcTemplate.update("""
                INSERT INTO ai_analysis_record (request_type, business_reference_id, initiator_employee_id, status,
                duration_ms, result_summary, audited_at) VALUES (?, ?, NULL, ?, ?, ?, ?)
                """, "APPROVAL", "legacy-99", "SUCCESS", 1L, "仅供参考：历史摘要",
                java.sql.Timestamp.valueOf(java.time.LocalDateTime.of(2026, 7, 23, 10, 0)));
        Long id = jdbcTemplate.queryForObject("SELECT id FROM ai_analysis_record WHERE business_reference_id = ?", Long.class, "legacy-99");

        AiAnalysisRecord found = recordMapper.findById(id);

        assertThat(found.getInitiatorEmployeeId()).isNull();
    }

    @TestConfiguration
    static class TestBeans {

        @Bean
        @Primary
        AiProvider testAiProvider() {
            return (AiPrompt prompt) -> new AiCallResult(
                    AiCallStatus.SUCCESS,
                    "model output SECRET_DISPLAY"
            );
        }
    }
}
