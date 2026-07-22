package com.tsy.oa.intelligence.ai.provider;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class JavaAiHttpTransport implements AiHttpTransport {

    private final HttpClient httpClient;

    public JavaAiHttpTransport() {
        this(HttpClient.newBuilder().build());
    }

    JavaAiHttpTransport(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public AiHttpResponse exchange(AiHttpRequest request) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(request.endpoint()))
                .timeout(request.timeout())
                .header("Authorization", "Bearer " + request.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(request.requestBody()))
                .build();
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        return new AiHttpResponse(response.statusCode(), response.body());
    }
}
