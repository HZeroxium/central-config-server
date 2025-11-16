package com.example.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * Global filter for request/response logging.
 * <p>
 * Logs incoming requests and outgoing responses with timing information.
 * </p>
 *
 * @since 1.0.0
 */
@Slf4j
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        Instant startTime = Instant.now();
        
        String method = request.getMethod().name();
        String path = request.getURI().getPath();
        String correlationId = request.getHeaders().getFirst("X-Correlation-ID");
        
        log.debug("Incoming request: {} {} [correlationId={}]", method, path, correlationId);
        
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            Instant endTime = Instant.now();
            Duration duration = Duration.between(startTime, endTime);
            
            log.info("Request completed: {} {} -> {} [correlationId={}, duration={}ms]",
                    method, path, response.getStatusCode(), correlationId, duration.toMillis());
        }));
    }

    @Override
    public int getOrder() {
        // Run after CorrelationIdFilter
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}

