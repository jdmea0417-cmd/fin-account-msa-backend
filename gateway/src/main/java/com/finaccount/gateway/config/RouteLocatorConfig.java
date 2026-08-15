package com.finaccount.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteLocatorConfig {
    @Bean
    public RouteLocator getRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("transaction-service-h2console", route -> route
                        .path("/transactions/h2-console/**")
                        .filters(filter -> filter
                                .rewritePath(
                                        "/transactions/(?<segment>.*)",
                                        "/${segment}"
                                ))
                        .uri("lb://TRANSACTION-SERVICE")
                )
                .route("transaction-service", route -> route
                        .path("/transactions/**")
                        .filters(filter -> filter)
                        .uri("lb://TRANSACTION-SERVICE")
                )
                .build();
    }
}
