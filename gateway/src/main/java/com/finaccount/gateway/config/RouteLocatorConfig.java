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
                .route("account-service-login", route -> route
                        .path("/auth/login/**")
                        .filters(filter -> filter
                                .rewritePath(
                                        "/auth/(?<segment>.*)",
                                        "/${segment}"
                                ))
                        .uri("lb://ACCOUNT-SERVICE")
                )
                .route("account-service-h2console", route -> route
                        .path("/account-service/h2-console/**")
                        .filters(filter -> filter
                                .rewritePath(
                                        "/account-service/(?<segment>.*)",
                                        "/${segment}"
                                ))
                        .uri("lb://ACCOUNT-SERVICE")
                )
                .route("account-service", route -> route
                        .path("/accounts/**")
                        .filters(filter -> filter)
                        .uri("lb://ACCOUNT-SERVICE")
                )
                .build();
    }
}
