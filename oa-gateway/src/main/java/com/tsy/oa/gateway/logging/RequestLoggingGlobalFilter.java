package com.tsy.oa.gateway.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

@Component
public class RequestLoggingGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLoggingGlobalFilter.class);
    private static final String EMPLOYEE_ID_HEADER = "X-Employee-Id";
    private static final String ANONYMOUS_EMPLOYEE = "anonymous";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startNanos = System.nanoTime();
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        String employeeId = exchange.getRequest().getHeaders().getFirst(EMPLOYEE_ID_HEADER);
        if (employeeId == null || employeeId.isBlank()) {
            employeeId = ANONYMOUS_EMPLOYEE;
        }

        String currentEmployeeId = employeeId;
        return chain.filter(exchange).doFinally(ignored -> {
            HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
            int status = statusCode == null ? 200 : statusCode.value();
            long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            LOGGER.info(
                    "Gateway request method={} path={} status={} durationMs={} employeeId={}",
                    method,
                    path,
                    status,
                    durationMillis,
                    currentEmployeeId
            );
        });
    }

    @Override
    public int getOrder() {
        return -90;
    }
}
