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
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountResponseDTO> createAccount(AccountRequestDTO accountRequestDTO) {
        AccountResponseDTO accountResponseDTO = accountService.createAccount(accountRequestDTO);

        return ResponseEntity.status(HttpStatus.OK).body(accountResponseDTO);
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<AccountResponseDTO> getAccount(@PathVariable Long accountId) {
        AccountResponseDTO accountResponseDTO = accountService.getAccount(accountId);

        return ResponseEntity.status(HttpStatus.OK).body(accountResponseDTO);
    }

    @GetMapping("/accounts/{accountId}/balance")
    public ResponseEntity<BalanceResponseDTO> getAccountBalance(@PathVariable Long accountId) {
        BalanceResponseDTO balanceResponseDTO = accountService.getAccountBalance(accountId);

        return ResponseEntity.status(HttpStatus.OK).body(balanceResponseDTO);
    }
}