package com.finaccount.transactionservice.service;

import com.finaccount.transactionservice.dto.TransactionRequestDTO;
import com.finaccount.transactionservice.dto.TransactionResponseDTO;
import com.finaccount.transactionservice.dto.TransactionStatus;
import com.finaccount.transactionservice.dto.TransactionType;
import com.finaccount.transactionservice.jpa.TransactionEntity;
import com.finaccount.transactionservice.jpa.TransactionRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public TransactionResponseDTO deposit(TransactionRequestDTO transactionRequestDTO) {
        TransactionEntity.TransactionEntityBuilder builder = TransactionEntity.builder();
        builder.fromAccountId(transactionRequestDTO.getFromAccountId());
        builder.toAccountId(transactionRequestDTO.getToAccountId());
        builder.amount(transactionRequestDTO.getAmount());
        builder.type(TransactionType.DEPOSIT);
        builder.status(TransactionStatus.SUCCESS);
        builder.createdAt(Instant.now());
        TransactionEntity transactionEntity = builder.build();

        TransactionEntity entity = transactionRepository.insert(transactionEntity);

        TransactionResponseDTO response = new ModelMapper().map(entity, TransactionResponseDTO.class);

        return response;
    }

    public TransactionResponseDTO withdraw(TransactionRequestDTO transactionRequestDTO) {
        TransactionEntity.TransactionEntityBuilder builder = TransactionEntity.builder();
        builder.fromAccountId(transactionRequestDTO.getFromAccountId());
        builder.toAccountId(transactionRequestDTO.getToAccountId());
        builder.amount(transactionRequestDTO.getAmount());
        builder.type(TransactionType.WITHDRAW);
        builder.status(TransactionStatus.SUCCESS);
        builder.createdAt(Instant.now());
        TransactionEntity transactionEntity = builder.build();

        TransactionEntity entity = transactionRepository.insert(transactionEntity);

        TransactionResponseDTO response = new ModelMapper().map(entity, TransactionResponseDTO.class);

        return response;
    }

    public TransactionResponseDTO transfer(TransactionRequestDTO transactionRequestDTO) {
        TransactionEntity.TransactionEntityBuilder builder = TransactionEntity.builder();
        builder.fromAccountId(transactionRequestDTO.getFromAccountId());
        builder.toAccountId(transactionRequestDTO.getToAccountId());
        builder.amount(transactionRequestDTO.getAmount());
        builder.type(TransactionType.TRANSFER);
        builder.status(TransactionStatus.SUCCESS);
        builder.createdAt(Instant.now());
        TransactionEntity transactionEntity = builder.build();

        TransactionEntity entity = transactionRepository.insert(transactionEntity);

        TransactionResponseDTO response = new ModelMapper().map(entity, TransactionResponseDTO.class);

        return response;
    }

    public TransactionResponseDTO getTransaction(Long transactionId) {
        TransactionEntity entity = transactionRepository.findByTransactionId(transactionId);

        TransactionResponseDTO transactionResponseDTO = new ModelMapper().map(entity, TransactionResponseDTO.class);

        return transactionResponseDTO;
    }

    public List<TransactionResponseDTO> getTransactions(Long accountId) {
        List<TransactionEntity> entities = transactionRepository.findByAccountId();

        List<TransactionResponseDTO> responses = entities.stream()
                .map(entity -> new ModelMapper().map(entity, TransactionResponseDTO.class)).toList();

        return responses;
    }
}
