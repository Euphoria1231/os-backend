package com.tsy.oa.intelligence.ai.provider;

import java.time.Duration;
import java.util.Map;

record AiHttpRequest(
        String endpoint,
        String apiKey,
        String requestBody,
        Duration timeout,
        Map<String, String> headers
) {

    AiHttpRequest {
        headers = Map.copyOf(headers);
    }
}
