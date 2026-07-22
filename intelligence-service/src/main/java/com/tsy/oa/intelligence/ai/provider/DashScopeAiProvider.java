package com.tsy.oa.intelligence.ai.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tsy.oa.intelligence.ai.AiProvider;
import com.tsy.oa.intelligence.ai.config.AiProperties;
import com.tsy.oa.intelligence.ai.model.AiCallResult;
import com.tsy.oa.intelligence.ai.model.AiCallStatus;
import com.tsy.oa.intelligence.ai.model.AiPrompt;

import java.io.IOException;
import java.net.http.HttpTimeoutException;

public class DashScopeAiProvider implements AiProvider {

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final AiHttpTransport httpTransport;

    public DashScopeAiProvider(
            AiProperties properties,
            ObjectMapper objectMapper,
            AiHttpTransport httpTransport
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpTransport = httpTransport;
    }

    @Override
    public AiCallResult generate(AiPrompt prompt) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            return result(AiCallStatus.DEGRADED, "AI credentials are not configured.");
        }
        try {
            AiHttpResponse response = httpTransport.exchange(new AiHttpRequest(
                    properties.getEndpoint(),
                    properties.getApiKey(),
                    requestBody(prompt),
                    properties.getTimeout()
            ));
            if (response.statusCode() == 429) {
                return result(AiCallStatus.DEGRADED, "AI service is rate limited. Please retry later.");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return result(AiCallStatus.FAILED, "AI service is temporarily unavailable.");
            }
            return extractResponse(response.responseBody());
        } catch (HttpTimeoutException exception) {
            return result(AiCallStatus.DEGRADED, "AI request timed out. Please retry later.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return result(AiCallStatus.DEGRADED, "AI request was interrupted. Please retry later.");
        } catch (IOException | IllegalArgumentException exception) {
            return result(AiCallStatus.FAILED, "AI service is temporarily unavailable.");
        }
    }

    private String requestBody(AiPrompt prompt) throws JsonProcessingException {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", properties.getModel());
        ArrayNode messages = request.putArray("messages");
        ObjectNode message = messages.addObject();
        message.put("role", "user");
        message.put("content", prompt.content());
        return objectMapper.writeValueAsString(request);
    }

    private AiCallResult extractResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices").path(0).path("message").path("content").asText("").trim();
            if (content.isEmpty()) {
                return result(AiCallStatus.FAILED, "AI returned no usable analysis.");
            }
            return result(AiCallStatus.SUCCESS, content);
        } catch (JsonProcessingException exception) {
            return result(AiCallStatus.FAILED, "AI returned an invalid response.");
        }
    }

    private AiCallResult result(AiCallStatus status, String text) {
        return new AiCallResult(status, text);
    }
}
