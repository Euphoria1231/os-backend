package com.tsy.oa.intelligence.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsy.oa.intelligence.ai.AiProvider;
import com.tsy.oa.intelligence.ai.provider.DashScopeAiProvider;
import com.tsy.oa.intelligence.ai.provider.JavaAiHttpTransport;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AiProperties.class)
public class AiProviderConfiguration {

    @Bean
    AiProvider aiProvider(
            AiProperties properties,
            ObjectMapper objectMapper
    ) {
        return new DashScopeAiProvider(properties, objectMapper, new JavaAiHttpTransport());
    }
}
