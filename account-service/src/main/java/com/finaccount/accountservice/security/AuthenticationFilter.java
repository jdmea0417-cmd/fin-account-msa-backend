package com.finaccount.accountservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finaccount.accountservice.dto.AccountDto;
import com.finaccount.accountservice.service.AccountService;
import com.finaccount.accountservice.vo.LoginRequest;
import com.finaccount.accountservice.vo.LoginResponse;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

public class AuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    private final AccountService accountService;
    private final Environment environment;

    public AuthenticationFilter(AuthenticationManager authenticationManager, AccountService accountService, Environment environment) {
        super(authenticationManager);
        this.accountService = accountService;
        this.environment = environment;
    }

    @Override
    public Authentication attemptAuthentication(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) throws AuthenticationException {
        try {
            LoginRequest loginRequest = new ObjectMapper().readValue(httpRequest.getInputStream(), LoginRequest.class);

            String accountNumber = loginRequest.getAccountNumber();
            String password = loginRequest.getPassword();
            List<GrantedAuthority> authorities = List.of();

            Authentication authentication = new UsernamePasswordAuthenticationToken(accountNumber, password, authorities);

            return getAuthenticationManager().authenticate(authentication);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void successfulAuthentication(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            FilterChain chain,
            Authentication authentication
    ) {
        try {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            httpResponse.setContentType("application/json");
            httpResponse.setCharacterEncoding("UTF-8");

            AccountDto accountDto = createAccountDto(authentication, accountService);
            String accountNumber = accountDto.getAccountNumber();
            Integer accountId =  accountDto.getAccountId();

            String secret = environment.getProperty("token.secret");
            String expiration = environment.getProperty("token.expiration-in-days");
            String token = createToken(accountNumber, secret, expiration);

            LoginResponse loginResponse = new LoginResponse();
            loginResponse.setAccountId(accountId);
            loginResponse.setAccessToken(token);

            new ObjectMapper().writeValue(httpResponse.getWriter(), loginResponse);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private AccountDto createAccountDto(Authentication authentication, AccountService accountService) {
        String accountNumber = ((User) authentication.getPrincipal()).getUsername();
        AccountDto accountDto = accountService.getAccountByAccountNumber(accountNumber);

        return accountDto;
    }

    private String createToken(String accountNumber, String secret, String expiration) {
        Instant now = Instant.now();
        Date exp = Date.from(now.plus(Long.parseLong(expiration), ChronoUnit.DAYS));
        Date issuedAt =  Date.from(now);

        byte[] secretInBytes = secret.getBytes(StandardCharsets.UTF_8);
        SecretKey secretKey = Keys.hmacShaKeyFor(secretInBytes);

        JwtBuilder builder = Jwts.builder();
        builder.subject(accountNumber);
        builder.expiration(exp);
        builder.issuedAt(issuedAt);
        builder.signWith(secretKey);
        String token = builder.compact();

        return token;
    }
}
