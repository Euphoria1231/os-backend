package com.tsy.oa.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.nacos.config.enabled=false",
                "spring.cloud.nacos.config.import-check.enabled=false",
                "security.jwt.secret=test-jwt-secret-key-with-at-least-thirty-two-bytes"
        }
)
class OaGatewayApplicationTests {

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void contextLoads() {
    }

    @Test
    void businessServiceRoutesAreConfigured() {
        List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions()
                .collectList()
                .block(Duration.ofSeconds(5));

        assertNotNull(routes);
        Map<String, String> expectedRoutes = Map.of(
                "user-service", "/api/user/**",
                "attendance-service", "/api/attendance/**",
                "flow-service", "/api/flow/**",
                "notice-service", "/api/notices/**"
        );

        expectedRoutes.forEach((serviceName, pathPattern) -> {
            Optional<RouteDefinition> matchingRoute = routes.stream()
                    .filter(definition -> serviceName.equals(definition.getId()))
                    .findFirst();
            assertTrue(matchingRoute.isPresent(), "缺少 Gateway 路由: " + serviceName);
            RouteDefinition route = matchingRoute.orElseThrow();

            assertEquals("lb://" + serviceName, route.getUri().toString());
            assertTrue(route.getPredicates().stream().anyMatch(predicate ->
                    "Path".equals(predicate.getName())
                            && predicate.getArgs().containsValue(pathPattern)));
        });
    }

    @Test
    void openApiRoutesAreConfigured() {
        List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions()
                .collectList()
                .block(Duration.ofSeconds(5));

        assertNotNull(routes);
        Map<String, List<String>> expectedRoutes = Map.of(
                "user-openapi", List.of("user-service", "/openapi/user"),
                "attendance-openapi", List.of("attendance-service", "/openapi/attendance"),
                "flow-openapi", List.of("flow-service", "/openapi/flow"),
                "notice-openapi", List.of("notice-service", "/openapi/notice")
        );

        expectedRoutes.forEach((routeId, expected) -> {
            RouteDefinition route = routes.stream()
                    .filter(definition -> routeId.equals(definition.getId()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("缺少 OpenAPI 路由: " + routeId));
            assertEquals("lb://" + expected.get(0), route.getUri().toString());
            assertTrue(route.getPredicates().stream().anyMatch(predicate ->
                    "Path".equals(predicate.getName())
                            && predicate.getArgs().containsValue(expected.get(1))));
            assertTrue(route.getFilters().stream().anyMatch(filter ->
                    "SetPath".equals(filter.getName())
                            && filter.getArgs().containsValue("/v3/api-docs")));
        });
    }

    @Test
    void servesAggregatedSwaggerUiConfiguration() {
        webTestClient.get()
                .uri("/swagger-ui.html")
                .exchange()
                .expectStatus().is3xxRedirection();

        webTestClient.get()
                .uri("/v3/api-docs/swagger-config")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.urls.length()").isEqualTo(4);
    }
}
