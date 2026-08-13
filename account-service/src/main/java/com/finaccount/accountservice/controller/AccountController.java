package com.finaccount.accountservice.controller;

import com.finaccount.accountservice.dto.AccountRequestDTO;
import com.finaccount.accountservice.dto.BalanceResponseDTO;
import com.finaccount.accountservice.dto.AccountResponseDTO;
import com.finaccount.accountservice.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountController {
    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountResponseDTO> createAccount(AccountRequestDTO request) {
        AccountResponseDTO response = service.createAccount(request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<AccountResponseDTO> getAccount(@PathVariable Long accountId) {
        AccountResponseDTO response = service.getAccount(accountId);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/accounts/{accountId}/balance")
    public ResponseEntity<BalanceResponseDTO> getBalance(@PathVariable Long accountId) {
        BalanceResponseDTO response = service.getBalance(accountId);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/internal/accounts/{id}/balance")
    public ResponseEntity<BalanceResponseDTO> getBalanceInternal(@PathVariable Long accountId) {
        BalanceResponseDTO response = service.getBalance(accountId);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // TODO
    // BalanceResponseDTO or AccountResponseDTO
    @PostMapping("/internal/accounts/{id}/deposit")
    public ResponseEntity<BalanceResponseDTO> depositInternal(@PathVariable Long accountId) {
        BalanceResponseDTO response = service.getBalance(accountId);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // TODO
    // BalanceResponseDTO or AccountResponseDTO
    @PostMapping("/internal/accounts/{id}/withdraw")
    public ResponseEntity<BalanceResponseDTO> withdrawInternal(@PathVariable Long accountId) {
        BalanceResponseDTO response = service.getBalance(accountId);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}