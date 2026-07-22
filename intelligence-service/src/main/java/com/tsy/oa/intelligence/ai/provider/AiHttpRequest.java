package com.tsy.oa.intelligence.ai.provider;

import java.time.Duration;

record AiHttpRequest(String endpoint, String apiKey, String requestBody, Duration timeout) {
}
