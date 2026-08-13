package com.finaccount.transactionservice.jpa;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface TransactionRepository extends CrudRepository<TransactionEntity, Long> {
    List<TransactionEntity> findByAccountId();

    TransactionEntity insert(TransactionEntity transactionEntity);

    TransactionEntity findByTransactionId(Long transactionId);
}
