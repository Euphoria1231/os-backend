package com.tsy.oa.apitest.client;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;

/**
 * Gateway HTTP 客户端 - 基于 JDK HttpClient，不依赖业务 Service 代码。
 */
public class GatewayClient implements AutoCloseable {

    private static final String DEFAULT_BASE_URL = "http://localhost:8088";
    private static final int DEFAULT_CONNECT_TIMEOUT = 10000;
    private static final int DEFAULT_READ_TIMEOUT = 15000;

    private final HttpClient httpClient;
    private final String baseUrl;
    private final int readTimeout;
    private final ObjectMapper objectMapper;
    private String accessToken;

    public GatewayClient() {
        this(loadBaseUrl(), DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    public GatewayClient(String baseUrl, int connectTimeout, int readTimeout) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.readTimeout = readTimeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeout))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    private static String loadBaseUrl() {
        String envUrl = System.getenv("OA_TEST_BASE_URL");
        if (envUrl != null && !envUrl.isBlank()) {
            return envUrl;
        }
        try (InputStream is = GatewayClient.class.getClassLoader()
                .getResourceAsStream("test.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                return props.getProperty("base.url", DEFAULT_BASE_URL);
            }
        } catch (Exception e) {
            // fall back to default
        }
        return DEFAULT_BASE_URL;
    }

    private static String normalizeBaseUrl(String url) {
        if (url == null) return DEFAULT_BASE_URL;
        url = url.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    public void setAccessToken(String token) {
        this.accessToken = token;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * 发送 GET 请求
     */
    public ApiResponse get(String path) {
        return sendRequest("GET", path, null);
    }

    /**
     * 发送 POST 请求
     */
    public ApiResponse post(String path, Object body) {
        return sendRequest("POST", path, body);
    }

    /**
     * 发送 PUT 请求
     */
    public ApiResponse put(String path, Object body) {
        return sendRequest("PUT", path, body);
    }

    /**
     * 发送 DELETE 请求
     */
    public ApiResponse delete(String path) {
        return sendRequest("DELETE", path, null);
    }

    /**
     * 返回原始 HTTP 响应（用于检查状态码等）
     */
    public HttpResponse<String> sendRaw(String method, String path, Object body) {
        try {
            URI uri = new URI(baseUrl + path);
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri);

            if ("GET".equalsIgnoreCase(method)) {
                builder.GET();
            } else if ("DELETE".equalsIgnoreCase(method)) {
                builder.DELETE();
            } else {
                String jsonBody = (body instanceof String)
                        ? (String) body
                        : objectMapper.writeValueAsString(body);
                HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.ofString(
                        jsonBody, StandardCharsets.UTF_8);
                builder.method(method.toUpperCase(), bodyPublisher);
                builder.header("Content-Type", "application/json");
            }

            if (accessToken != null && !accessToken.isBlank()) {
                builder.header("Authorization", "Bearer " + accessToken);
            }

            builder.timeout(Duration.ofMillis(readTimeout));

            return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("HTTP " + method + " " + path + " failed: " + e.getMessage(), e);
        }
    }

    private ApiResponse sendRequest(String method, String path, Object body) {
        try {
            HttpResponse<String> response = sendRaw(method, path, body);
            String responseBody = response.body();

            if (responseBody == null || responseBody.isBlank()) {
                ApiResponse errorResp = new ApiResponse();
                errorResp.setCode(response.statusCode());
                errorResp.setMessage("Empty response body");
                return errorResp;
            }

            // Try to parse as ApiResponse first
            try {
                return objectMapper.readValue(responseBody, ApiResponse.class);
            } catch (Exception e) {
                // If response is not ApiResponse format, wrap it
                ApiResponse fallback = new ApiResponse();
                fallback.setCode(response.statusCode());
                fallback.setMessage(responseBody.length() > 200
                        ? responseBody.substring(0, 200) + "..."
                        : responseBody);
                return fallback;
            }
        } catch (Exception e) {
            ApiResponse errorResp = new ApiResponse();
            errorResp.setCode(-1);
            errorResp.setMessage("Request failed: " + e.getMessage());
            return errorResp;
        }
    }

    @Override
    public void close() {
        // HttpClient instances are pooled and don't need explicit closing in JDK 21
    }
}
