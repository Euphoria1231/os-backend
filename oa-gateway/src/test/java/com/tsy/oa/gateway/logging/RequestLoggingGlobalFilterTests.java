package com.tsy.oa.gateway.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(OutputCaptureExtension.class)
class RequestLoggingGlobalFilterTests {

    @Test
    void logsRequestSummaryWithoutSensitiveData(CapturedOutput output) {
        RequestLoggingGlobalFilter filter = new RequestLoggingGlobalFilter();
        assertEquals(-90, filter.getOrder());
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/user/auth/login?password=query-secret")
                .header(HttpHeaders.AUTHORIZATION, "Bearer secret-token")
                .header("X-Employee-Id", "10")
                .body("{\"password\":\"body-secret\"}");
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, currentExchange -> {
            currentExchange.getResponse().setStatusCode(HttpStatus.CREATED);
            return Mono.empty();
        }).block();

        String logs = output.getOut();
        assertTrue(logs.contains("method=POST"));
        assertTrue(logs.contains("path=/api/user/auth/login"));
        assertTrue(logs.contains("status=201"));
        assertTrue(logs.contains("durationMs="));
        assertTrue(logs.contains("employeeId=10"));
        assertFalse(logs.contains("secret-token"));
        assertFalse(logs.contains("query-secret"));
        assertFalse(logs.contains("body-secret"));
    }
}
