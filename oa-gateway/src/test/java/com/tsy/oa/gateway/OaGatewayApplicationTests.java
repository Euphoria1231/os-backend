package com.tsy.oa.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;

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
}
