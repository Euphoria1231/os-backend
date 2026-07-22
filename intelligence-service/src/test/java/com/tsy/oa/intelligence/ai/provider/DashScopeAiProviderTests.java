package com.tsy.oa.intelligence.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsy.oa.intelligence.ai.config.AiProperties;
import com.tsy.oa.intelligence.ai.model.AiCallStatus;
import com.tsy.oa.intelligence.ai.model.AiPrompt;
import org.junit.jupiter.api.Test;

import java.net.http.HttpTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class DashScopeAiProviderTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void successfulResponseExtractsModelTextAndMarksItAsReferenceOnly() {
        DashScopeAiProvider provider = provider((request) -> new AiHttpResponse(
                200,
                "{\"choices\":[{\"message\":{\"content\":\"建议先核对考勤数据\"}}]}"
        ));

        var result = provider.generate(new AiPrompt("ATTENDANCE", "attendance-42", "请分析数据"));

        assertThat(result.status()).isEqualTo(AiCallStatus.SUCCESS);
        assertThat(result.displayText()).isEqualTo("仅供参考：建议先核对考勤数据");
    }

    @Test
    void timeoutReturnsSafeDegradedResultWithoutPromptContent() {
        DashScopeAiProvider provider = provider((request) -> {
            throw new HttpTimeoutException("timeout");
        });

        var result = provider.generate(new AiPrompt("ATTENDANCE", "attendance-42", "敏感提示词"));

        assertThat(result.status()).isEqualTo(AiCallStatus.DEGRADED);
        assertThat(result.displayText()).contains("仅供参考").doesNotContain("敏感提示词");
    }

    @Test
    void rateLimitedResponseReturnsSafeDegradedResult() {
        DashScopeAiProvider provider = provider((request) -> new AiHttpResponse(429, "{\"error\":{}}"));

        var result = provider.generate(new AiPrompt("ATTENDANCE", "attendance-42", "敏感提示词"));

        assertThat(result.status()).isEqualTo(AiCallStatus.DEGRADED);
        assertThat(result.displayText()).contains("仅供参考").doesNotContain("敏感提示词");
    }

    @Test
    void invalidResponseReturnsSafeFailureResult() {
        DashScopeAiProvider provider = provider((request) -> new AiHttpResponse(200, "not-json"));

        var result = provider.generate(new AiPrompt("ATTENDANCE", "attendance-42", "敏感提示词"));

        assertThat(result.status()).isEqualTo(AiCallStatus.FAILED);
        assertThat(result.displayText()).contains("仅供参考").doesNotContain("敏感提示词");
    }

    @Test
    void missingApiKeyReturnsSafeDegradedResultWithoutMakingHttpCall() {
        AiProperties properties = new AiProperties();
        properties.setApiKey("");
        DashScopeAiProvider provider = new DashScopeAiProvider(properties, objectMapper, request -> {
            throw new AssertionError("HTTP must not be invoked without credentials");
        });

        var result = provider.generate(new AiPrompt("ATTENDANCE", "attendance-42", "敏感提示词"));

        assertThat(result.status()).isEqualTo(AiCallStatus.DEGRADED);
        assertThat(result.displayText()).contains("仅供参考").doesNotContain("敏感提示词");
    }

    private DashScopeAiProvider provider(AiHttpTransport transport) {
        AiProperties properties = new AiProperties();
        properties.setApiKey("test-api-key");
        return new DashScopeAiProvider(properties, objectMapper, transport);
    }
}
