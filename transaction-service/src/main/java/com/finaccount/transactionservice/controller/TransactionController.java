package com.finaccount.transactionservice.controller;

import com.finaccount.transactionservice.dto.TransactionDto;
import com.finaccount.transactionservice.vo.TransactionRequest;
import com.finaccount.transactionservice.vo.TransactionResponse;
import com.finaccount.transactionservice.service.TransactionService;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
public class TransactionController {
    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @PostMapping("/transactions/deposit")
    public ResponseEntity<TransactionResponse> deposit(@RequestBody TransactionRequest request) {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        TransactionDto dto = mapper.map(request, TransactionDto.class);

        TransactionDto deposited = service.deposit(dto);

        TransactionResponse response = mapper.map(deposited, TransactionResponse.class);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/transactions/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(@RequestBody TransactionRequest request) {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        TransactionDto dto = mapper.map(request, TransactionDto.class);

        TransactionDto withdrawn = service.withdraw(dto);

        TransactionResponse response = mapper.map(withdrawn, TransactionResponse.class);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/transactions/transfer")
    public ResponseEntity<TransactionResponse> transfer(@RequestBody TransactionRequest request) {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        TransactionDto dto = mapper.map(request, TransactionDto.class);

        TransactionDto transferred = service.transfer(dto);

        TransactionResponse response = mapper.map(transferred, TransactionResponse.class);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable Integer transactionId) {
        try {
            TransactionDto dto = service.getTransaction(transactionId);

            ModelMapper mapper = new ModelMapper();
            mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

            TransactionResponse response = mapper.map(dto, TransactionResponse.class);

            return ResponseEntity.status(HttpStatus.OK).body(response);

        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactions(@RequestParam Integer accountId) {
        try {
            List<TransactionDto> dtos = service.getTransactions(accountId);

            ModelMapper mapper = new ModelMapper();
            mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

            List<TransactionResponse> responses = dtos.stream()
                    .map(dto -> mapper.map(dto, TransactionResponse.class)).toList();

            return ResponseEntity.status(HttpStatus.OK).body(responses);

        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(List.of());
        }
    }
}
