package com.tsy.oa.intelligence.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsy.oa.intelligence.ai.config.AiProperties;
import com.tsy.oa.intelligence.ai.model.AiCallStatus;
import com.tsy.oa.intelligence.ai.model.AiPrompt;
import org.junit.jupiter.api.Test;

import java.net.http.HttpTimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DashScopeAiProviderTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void openAiCompatibleRequestUsesVveaiDefaultsAndCustomHeader() throws Exception {
        AiProperties properties = new AiProperties();
        properties.setApiKey("test-api-key");
        AtomicReference<AiHttpRequest> capturedRequest = new AtomicReference<>();
        DashScopeAiProvider provider = new DashScopeAiProvider(properties, objectMapper, request -> {
            capturedRequest.set(request);
            return new AiHttpResponse(
                    200,
                    "{\"choices\":[{\"message\":{\"content\":\"Hello there!\"}}]}"
            );
        });

        provider.generate(new AiPrompt("OFFICE_QA", "question-1", "Hello world!"));

        AiHttpRequest request = capturedRequest.get();
        assertThat(request.endpoint()).isEqualTo("https://api.vveai.com/chat/completions");
        assertThat(request.apiKey()).isEqualTo("test-api-key");
        assertThat(request.headers()).containsEntry("x-foo", "true");
        var requestBody = objectMapper.readTree(request.requestBody());
        assertThat(requestBody.path("model").asText()).isEqualTo("gemini-3.5-flash-lite");
        assertThat(requestBody.path("messages").path(0).path("role").asText()).isEqualTo("user");
        assertThat(requestBody.path("messages").path(0).path("content").asText()).isEqualTo("Hello world!");
    }

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
    void responseWithoutChoicesReturnsSafeFailureResult() {
        DashScopeAiProvider provider = provider((request) -> new AiHttpResponse(200, "{\"id\":\"request-id\"}"));

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
