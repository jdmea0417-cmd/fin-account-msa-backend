package com.finaccount.transactionservice.controller;

import com.finaccount.transactionservice.dto.TransactionRequestDTO;
import com.finaccount.transactionservice.dto.TransactionResponseDTO;
import com.finaccount.transactionservice.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transaction/deposit")
    public ResponseEntity<TransactionResponseDTO> deposit(TransactionRequestDTO transactionRequestDTO) {
        TransactionResponseDTO response = transactionService.deposit(transactionRequestDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/transaction/withdraw")
    public ResponseEntity<TransactionResponseDTO> withdraw(TransactionRequestDTO transactionRequestDTO) {
        TransactionResponseDTO response = transactionService.withdraw(transactionRequestDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/transaction/transfer")
    public ResponseEntity<TransactionResponseDTO> transfer(TransactionRequestDTO transactionRequestDTO) {
        TransactionResponseDTO response = transactionService.transfer(transactionRequestDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<TransactionResponseDTO> getTransaction(@PathVariable Long transactionId) {
        TransactionResponseDTO response = transactionService.getTransaction(transactionId);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/transaction")
    public ResponseEntity<List<TransactionResponseDTO>> getTransactions(@RequestParam Long accountId) {
        List<TransactionResponseDTO> responses = transactionService.getTransactions(accountId);

        return ResponseEntity.status(HttpStatus.OK).body(responses);
    }
}
