package com.finaccount.transactionservice.jpa;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface TransactionRepository extends CrudRepository<TransactionEntity, Integer> {
    @Query("""
            SELECT t FROM TransactionEntity t
            WHERE t.fromAccountId = :accountId OR t.toAccountId = :accountId
            """)
    List<TransactionEntity> findByAccountId(Integer accountId);
}
