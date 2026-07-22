package com.tsy.oa.intelligence.search.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsy.oa.intelligence.search.repository.ElasticsearchGateway;
import com.tsy.oa.intelligence.search.repository.RestElasticsearchGateway;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ElasticsearchSearchProperties.class)
public class ElasticsearchConfiguration {

    @Bean(destroyMethod = "close")
    public RestClient elasticsearchRestClient(ElasticsearchSearchProperties properties) {
        return RestClient.builder(HttpHost.create(properties.getUrl())).build();
    }

    @Bean
    public ElasticsearchGateway elasticsearchGateway(
            RestClient restClient,
            ObjectMapper objectMapper
    ) {
        return new RestElasticsearchGateway(restClient, objectMapper);
    }
}
