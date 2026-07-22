package com.tsy.oa.intelligence.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration(proxyBeanMethods = false)
public class IntelligenceOpenApiConfiguration {

    @Bean
    public OpenAPI intelligenceOpenApi(
            @Value("${openapi.server-url:http://localhost:8088}") String serverUrl
    ) {
        return new OpenAPI()
                .info(new Info().title("OA 搜索与智能平台 API").version("v1"))
                .servers(List.of(new Server().url(serverUrl)));
    }
}
