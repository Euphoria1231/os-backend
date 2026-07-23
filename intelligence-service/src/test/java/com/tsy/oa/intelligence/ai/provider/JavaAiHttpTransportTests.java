package com.tsy.oa.intelligence.ai.provider;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JavaAiHttpTransportTests {

    @Test
    void exchangeForwardsAuthorizationAndCustomHeaders() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> customHeader = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            customHeader.set(exchange.getRequestHeaders().getFirst("x-foo"));
            byte[] response = "{\"choices\":[]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            try (var responseBody = exchange.getResponseBody()) {
                responseBody.write(response);
            }
        });
        server.start();

        try {
            AiHttpResponse response = new JavaAiHttpTransport().exchange(new AiHttpRequest(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/chat/completions",
                    "test-api-key",
                    "{}",
                    Duration.ofSeconds(2),
                    Map.of("x-foo", "true")
            ));

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(authorization.get()).isEqualTo("Bearer test-api-key");
            assertThat(customHeader.get()).isEqualTo("true");
        } finally {
            server.stop(0);
        }
    }
}
