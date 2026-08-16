package com.finaccount.transactionservice.service;

import com.finaccount.transactionservice.client.AccountServiceClient;
import com.finaccount.transactionservice.dto.TransactionDto;
import com.finaccount.transactionservice.dto.TransactionStatus;
import com.finaccount.transactionservice.jpa.TransactionEntity;
import com.finaccount.transactionservice.jpa.TransactionRepository;
import com.finaccount.transactionservice.vo.AccountRequest;
import com.finaccount.transactionservice.vo.AccountResponse;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class TransactionService {
    private final TransactionRepository repository;
    private final AccountServiceClient accountService;

    public TransactionService(
            TransactionRepository repository,
            AccountServiceClient accountService
    ) {
        this.repository = repository;
        this.accountService = accountService;
    }

    public TransactionDto addTransaction(TransactionDto dto) {
        try {
            TransactionDto success = addSuccessTransaction(dto);

            return success;

        } catch (ResponseStatusException e) {
            TransactionDto failed = addFailedTransaction(dto);

            return failed;
        }
    }

    @Transactional
    private TransactionDto addSuccessTransaction(TransactionDto dto) {
        ModelMapper mapper = new ModelMapper();
        TransactionEntity entity = mapper.map(dto, TransactionEntity.class);

        entity.setType(dto.getType());
        entity.setStatus(TransactionStatus.PENDING);
        entity.setCreatedAt(Instant.now());
        entity = repository.save(entity);

        this.updateAccount(dto);

        entity.setStatus(TransactionStatus.SUCCESS);
        entity = repository.save(entity);

        TransactionDto success = mapper.map(entity, TransactionDto.class);

        return success;
    }

    @Transactional
    private TransactionDto addFailedTransaction(TransactionDto dto) {
        ModelMapper mapper = new ModelMapper();
        TransactionEntity entity = mapper.map(dto, TransactionEntity.class);

        entity.setType(dto.getType());
        entity.setStatus(TransactionStatus.FAILED);
        entity.setCreatedAt(Instant.now());
        entity = repository.save(entity);

        TransactionDto failed = mapper.map(entity, TransactionDto.class);

        return failed;
    }

    public TransactionDto getTransaction(Integer transactionId) throws NoSuchElementException {
        TransactionEntity entity = repository.findById(transactionId).orElseThrow();

        ModelMapper mapper = new ModelMapper();
        TransactionDto dto = mapper.map(entity, TransactionDto.class);

        return dto;
    }

    public List<TransactionDto> getTransactions(Integer accountId) {
        List<TransactionEntity> entities = repository.findByAccountId(accountId);

        ModelMapper mapper = new ModelMapper();
        List<TransactionDto> dtos = entities.stream()
                .map(entity -> mapper.map(entity, TransactionDto.class))
                .toList();

        return dtos;
    }

    private void updateAccount(TransactionDto transaction) throws ResponseStatusException {
        switch (transaction.getType()) {
            case DEPOSIT -> updateAccountForDepositTransaction(transaction);
            case WITHDRAW -> updateAccountForWithdrawTransaction(transaction);
            case TRANSFER -> updateAccountForTransferTransaction(transaction);
        }
    }

    private void updateAccountForDepositTransaction(TransactionDto transaction) {
        Integer accountId = transaction.getToAccountId();
        Long amount = transaction.getAmount();

        AccountResponse response = accountService.getAccount(accountId);

        AccountRequest.AccountRequestBuilder builder = AccountRequest.builder();
        builder.balance(response.getBalance() + amount);
        AccountRequest request = builder.build();

        accountService.updateAccount(accountId, request);
    }

    private void updateAccountForWithdrawTransaction(TransactionDto transaction) {
        Integer accountId = transaction.getFromAccountId();
        Long amount = transaction.getAmount();

        AccountResponse response = accountService.getAccount(accountId);

        AccountRequest.AccountRequestBuilder builder = AccountRequest.builder();
        builder.balance(response.getBalance() - amount);
        AccountRequest request = builder.build();

        accountService.updateAccount(accountId, request);
    }

    private void updateAccountForTransferTransaction(TransactionDto transaction) {
        updateAccountForDepositTransaction(transaction);
        updateAccountForWithdrawTransaction(transaction);
    }
}
