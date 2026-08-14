package com.finaccount.transactionservice.service;

import com.finaccount.transactionservice.dto.TransactionDto;
import com.finaccount.transactionservice.dto.TransactionStatus;
import com.finaccount.transactionservice.dto.TransactionType;
import com.finaccount.transactionservice.jpa.TransactionEntity;
import com.finaccount.transactionservice.jpa.TransactionRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class TransactionService {
    private final TransactionRepository repository;

    public TransactionService(TransactionRepository repository) {
        this.repository = repository;
    }

    public TransactionDto deposit(TransactionDto dto) {
        ModelMapper mapper = new ModelMapper();
        TransactionEntity entity = mapper.map(dto, TransactionEntity.class);

        entity.setType(TransactionType.DEPOSIT);
        entity.setStatus(TransactionStatus.PENDING);
        entity.setCreatedAt(Instant.now());

        repository.save(entity);

        TransactionDto saved =  mapper.map(entity, TransactionDto.class);

        return saved;
    }

    public TransactionDto withdraw(TransactionDto dto) {
        ModelMapper mapper = new ModelMapper();
        TransactionEntity entity = mapper.map(dto, TransactionEntity.class);

        entity.setType(TransactionType.WITHDRAW);
        entity.setStatus(TransactionStatus.PENDING);
        entity.setCreatedAt(Instant.now());

        repository.save(entity);

        TransactionDto saved =  mapper.map(entity, TransactionDto.class);

        return saved;
    }

    public TransactionDto transfer(TransactionDto dto) {
        ModelMapper mapper = new ModelMapper();
        TransactionEntity entity = mapper.map(dto, TransactionEntity.class);

        entity.setType(TransactionType.TRANSFER);
        entity.setStatus(TransactionStatus.PENDING);
        entity.setCreatedAt(Instant.now());

        repository.save(entity);

        TransactionDto saved =  mapper.map(entity, TransactionDto.class);

        return saved;
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
}
