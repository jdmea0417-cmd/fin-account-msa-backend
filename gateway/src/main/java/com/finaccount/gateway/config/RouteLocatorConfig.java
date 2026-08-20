package com.finaccount.gateway.config;

import com.finaccount.gateway.filter.AuthorizationHeaderFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

@Configuration
public class RouteLocatorConfig {

    private final AuthorizationHeaderFilter authFilter;

    public RouteLocatorConfig(AuthorizationHeaderFilter authFilter) {
        this.authFilter = authFilter;
    }

    @Bean
    public RouteLocator getRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // 1. Auth & Login (Public)
                .route("account-service-login", route -> route
                        .path("/auth/login/**")
                        .filters(filter -> filter
                                .rewritePath(
                                        "/auth/(?<segment>.*)",
                                        "/${segment}"
                                ))
                        .uri("lb://ACCOUNT-SERVICE")
                )
                // 2. Account Creation (Public)
                .route("account-service-create", route -> route
                        .path("/accounts")
                        .and()
                        .method(HttpMethod.POST)
                        .uri("lb://ACCOUNT-SERVICE")
                )
                // 3. H2 Consoles (Dev/Testing)
                .route("account-service-h2console", route -> route
                        .path("/account-service/h2-console/**")
                        .filters(filter -> filter
                                .rewritePath(
                                        "/account-service/(?<segment>.*)",
                                        "/${segment}"
                                ))
                        .uri("lb://ACCOUNT-SERVICE")
                )
                .route("transaction-service-h2console", route -> route
                        .path("/transaction-service/h2-console/**")
                        .filters(filter -> filter
                                .rewritePath(
                                        "/transaction-service/(?<segment>.*)",
                                        "/${segment}"
                                ))
                        .uri("lb://TRANSACTION-SERVICE")
                )
                .route("notification-service-h2console", route -> route
                        .path("/notification-service/h2-console/**")
                        .filters(filter -> filter
                                .rewritePath(
                                        "/notification-service/(?<segment>.*)",
                                        "/${segment}"
                                ))
                        .uri("lb://NOTIFICATION-SERVICE")
                )
                // 4. Account Service (Protected with JWT)
                .route("account-service-protected", route -> route
                        .path("/accounts/**")
                        .filters(filter -> filter.filter(authFilter.apply(new AuthorizationHeaderFilter.Config())))
                        .uri("lb://ACCOUNT-SERVICE")
                )
                // 5. Transaction Service (Protected with JWT)
                .route("transaction-service-protected", route -> route
                        .path("/transactions/**")
                        .filters(filter -> filter.filter(authFilter.apply(new AuthorizationHeaderFilter.Config())))
                        .uri("lb://TRANSACTION-SERVICE")
                )
                // 6. Notification Service (Protected with JWT)
                .route("notification-service-protected", route -> route
                        .path("/notifications/**")
                        .filters(filter -> filter.filter(authFilter.apply(new AuthorizationHeaderFilter.Config())))
                        .uri("lb://NOTIFICATION-SERVICE")
                )
                .build();
    }
}