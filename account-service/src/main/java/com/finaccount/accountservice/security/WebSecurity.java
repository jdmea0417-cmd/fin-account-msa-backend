package com.finaccount.accountservice.security;

import com.finaccount.accountservice.service.AccountService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurity {
    private final AccountService accountService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final Environment environment;

    public WebSecurity(
            AccountService accountService,
            BCryptPasswordEncoder bCryptPasswordEncoder,
            Environment environment
    ) {
        this.accountService = accountService;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.environment = environment;
    }

    @Bean
    public SecurityFilterChain configure(HttpSecurity http) throws Exception {
        AuthenticationManager authenticationManager = getAuthenticationManager(http, accountService, bCryptPasswordEncoder);

        http.csrf(getCsrfCustomizer());

        http.authorizeHttpRequests(getAuthorizeHttpRequestCustomizer());

        http.authenticationManager(getAuthenticationManager(authenticationManager));

        http.addFilter(getAuthenticationFilter(authenticationManager, accountService, environment));

        http.httpBasic(getHttpBasicCustomizer());

        http.headers(getHeadersCustomizer());

        return http.build();
    }

    private AuthenticationManager getAuthenticationManager(
            HttpSecurity http,
            AccountService accountService,
            BCryptPasswordEncoder bCryptPasswordEncoder
    ) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.userDetailsService(accountService).passwordEncoder(bCryptPasswordEncoder);
        AuthenticationManager authenticationManager = authenticationManagerBuilder.build();

        return authenticationManager;
    }

    private Customizer<CsrfConfigurer<HttpSecurity>> getCsrfCustomizer() {
        return AbstractHttpConfigurer::disable;
    }

    // TODO
    private Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>
            .AuthorizationManagerRequestMatcherRegistry>
    getAuthorizeHttpRequestCustomizer() {
        return (registry) -> {
            registry.requestMatchers("/h2-console/**").permitAll();
            registry.requestMatchers("/actuator/**").permitAll();
            registry.requestMatchers("/health-check/**").permitAll();
            registry.requestMatchers("/welcome/**").permitAll();
            registry.requestMatchers(HttpMethod.POST, "/accounts").permitAll();
            registry.anyRequest().permitAll();
        };
    }

    private AuthenticationManager getAuthenticationManager(AuthenticationManager authenticationManager) {
        return authenticationManager;
    }

    private AuthenticationFilter getAuthenticationFilter(
            AuthenticationManager authenticationManager,
            AccountService accountService,
            Environment environment
    ) {
        AuthenticationFilter authenticationFilter = new AuthenticationFilter(
                authenticationManager,
                accountService,
                environment
        );
        authenticationFilter.setAuthenticationManager(authenticationManager);
        return authenticationFilter;
    }

    private Customizer<HttpBasicConfigurer<HttpSecurity>> getHttpBasicCustomizer() {
        return Customizer.withDefaults();
    }

    private Customizer<HeadersConfigurer<HttpSecurity>> getHeadersCustomizer() {
        return (configurer) -> {
            configurer.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin);
        };
    }
}
